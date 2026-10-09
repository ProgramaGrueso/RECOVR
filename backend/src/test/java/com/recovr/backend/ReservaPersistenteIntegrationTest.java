package com.recovr.backend;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.security.CustomUserDetailsService;
import com.recovr.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración de la capa persistente sobre H2: JWT real, reglas por rol,
 * consultas JPQL de solapamiento/disponibilidad y códigos HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservaPersistenteIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;

    private String tokenAdmin;
    private String tokenRecepcion;
    private String tokenCliente;
    private Cliente cliente;
    private Empleado empleado;
    private Empleado empleado2;
    private Servicio servicio;   // 60 min + 15 min de limpieza
    private Sala sala;
    private Sala sala2;
    private LocalDateTime manana10;

    @BeforeEach
    void setUp() {
        manana10 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        tokenAdmin = token("admin@test.com", Rol.ADMIN);
        tokenRecepcion = token("recepcion@test.com", Rol.RECEPCIONISTA);
        tokenCliente = token("cliente@test.com", Rol.CLIENTE);

        cliente = new Cliente();
        cliente.setNombre("Cliente Prueba");
        cliente.setCorreo("cliente@test.com");
        cliente.setUsuarioId(usuarioRepository.findByCorreo("cliente@test.com").orElseThrow().getId());
        cliente = clienteRepository.save(cliente);

        empleado = guardarEmpleado("Especialista A");
        empleado2 = guardarEmpleado("Especialista B");
        sala = guardarSala("Cabina 1");
        sala2 = guardarSala("Cabina 2");

        servicio = new Servicio();
        servicio.setNombre("Crioterapia");
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);
        servicio.setPrecio(new BigDecimal("120.00"));
        servicio = servicioRepository.save(servicio);
    }

    // ---------- Solapamiento (Tarea 1) ----------

    @Test
    @DisplayName("Reserva que se cruza con otra (misma sala) devuelve 409; tras duración + limpieza se acepta")
    void solapamientoPorSala() throws Exception {
        crearReserva(tokenCliente, empleado, sala, manana10).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.finBloque").value(fmt(manana10.plusMinutes(75))));

        // 11:00 cae dentro de la limpieza (10:00 + 60 + 15 = 11:15)
        crearReserva(tokenCliente, empleado2, sala, manana10.plusMinutes(60))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sala")));

        crearReserva(tokenCliente, empleado2, sala, manana10.plusMinutes(75)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("El mismo especialista no puede atender dos sesiones que se cruzan aunque sean en salas distintas")
    void solapamientoPorEspecialista() throws Exception {
        crearReserva(tokenCliente, empleado, sala, manana10).andExpect(status().isCreated());
        crearReserva(tokenCliente, empleado, sala2, manana10.plusMinutes(30))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("especialista")));
    }

    @Test
    @DisplayName("Una reserva que empieza antes y termina dentro del intervalo también se detecta")
    void solapamientoQueEmpiezaAntes() throws Exception {
        crearReserva(tokenCliente, empleado, sala, manana10).andExpect(status().isCreated());
        crearReserva(tokenCliente, empleado2, sala, manana10.minusMinutes(30)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Las reservas CANCELADAS no bloquean el horario")
    void canceladaNoBloquea() throws Exception {
        guardarReserva(empleado, sala, manana10, EstadoReserva.CANCELADA);
        crearReserva(tokenCliente, empleado, sala, manana10.plusMinutes(30)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Actualizar una reserva sin cambiar su horario no choca consigo misma")
    void actualizarNoChocaConsigoMisma() throws Exception {
        Reserva r = guardarReserva(empleado, sala, manana10, EstadoReserva.PENDIENTE);
        mockMvc.perform(auth(put("/api/db/reservas/" + r.getId()), tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(empleado, sala, manana10, "\"estado\":\"CONFIRMADA\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("Fecha en el pasado devuelve 400 y IDs inexistentes devuelven 404")
    void validacionesDeReserva() throws Exception {
        crearReserva(tokenCliente, empleado, sala, LocalDateTime.now().minusDays(1)).andExpect(status().isBadRequest());
        mockMvc.perform(auth(post("/api/db/reservas"), tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoId\":9999,\"servicioId\":" + servicio.getId() + ",\"salaId\":" + sala.getId()
                                + ",\"fechaHora\":\"" + fmt(manana10) + "\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(post("/api/db/reservas"), tokenCliente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaHora\":\"" + fmt(manana10) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.salaId").exists());
    }

    // ---------- Disponibilidad JPQL (Tarea 2) ----------

    @Test
    @DisplayName("Disponibilidad de salas considera duración y limpieza de reservas que empezaron antes")
    void disponibilidadDeSalas() throws Exception {
        guardarReserva(empleado, sala, manana10, EstadoReserva.CONFIRMADA); // ocupa 10:00–11:15

        mockMvc.perform(auth(get("/api/salas/disponibles"), tokenAdmin)
                        .param("inicio", fmt(manana10.plusMinutes(30))).param("fin", fmt(manana10.plusMinutes(45))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(sala.getId().intValue()))))
                .andExpect(jsonPath("$[*].id", hasItem(sala2.getId().intValue())));

        mockMvc.perform(auth(get("/api/salas/disponibles"), tokenAdmin)
                        .param("inicio", fmt(manana10.plusMinutes(75))).param("fin", fmt(manana10.plusMinutes(120))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(sala.getId().intValue())));
    }

    @Test
    @DisplayName("Disponibilidad de especialistas excluye al ocupado y valida el rango")
    void disponibilidadDeEspecialistas() throws Exception {
        guardarReserva(empleado, sala, manana10, EstadoReserva.PENDIENTE);
        mockMvc.perform(auth(get("/api/empleados/disponibles"), tokenAdmin)
                        .param("inicio", fmt(manana10.plusMinutes(50))).param("fin", fmt(manana10.plusMinutes(70))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(empleado.getId().intValue()))))
                .andExpect(jsonPath("$[*].id", hasItem(empleado2.getId().intValue())));

        mockMvc.perform(auth(get("/api/empleados/disponibles"), tokenAdmin)
                        .param("inicio", fmt(manana10)).param("fin", fmt(manana10.minusHours(1))))
                .andExpect(status().isBadRequest());
    }

    // ---------- Seguridad /api/db/reservas (Tarea 3) y códigos (Tarea 4) ----------

    @Test
    @DisplayName("CLIENTE no puede listar, ver ni borrar todas las reservas; sí sus propias")
    void reglasPorRolEnReservasDb() throws Exception {
        Reserva r = guardarReserva(empleado, sala, manana10, EstadoReserva.PENDIENTE);

        mockMvc.perform(auth(get("/api/db/reservas"), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/db/reservas/" + r.getId()), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(auth(delete("/api/db/reservas/" + r.getId()), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/db/reservas/mias"), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(r.getId()));

        mockMvc.perform(auth(get("/api/db/reservas"), tokenRecepcion)).andExpect(status().isOk());
        mockMvc.perform(get("/api/db/reservas")).andExpect(status().isUnauthorized());
        mockMvc.perform(auth(delete("/api/db/reservas/" + r.getId()), tokenRecepcion)).andExpect(status().isNoContent());
        mockMvc.perform(auth(delete("/api/db/reservas/" + r.getId()), tokenRecepcion)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un CLIENTE autenticado sin permiso recibe 403 (no 401) y los datos inválidos 400")
    void codigosHttpCorrectos() throws Exception {
        mockMvc.perform(auth(get("/api/clientes"), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(auth(post("/api/servicios"), tokenCliente).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"duracionMinutos\":30,\"precio\":10}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"no-es-correo\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"cliente@test.com\",\"password\":\"incorrecta\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(auth(get("/api/servicios/9999"), tokenAdmin)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CRUD de catálogo responde 201 al crear y 204 al eliminar; valida el DTO")
    void crudDeCatalogo() throws Exception {
        String body = mockMvc.perform(auth(post("/api/servicios"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sauna\",\"duracionMinutos\":45,\"tiempoLimpiezaMinutos\":10,\"precio\":80.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mockMvc.perform(auth(delete("/api/servicios/" + id), tokenAdmin)).andExpect(status().isNoContent());

        mockMvc.perform(auth(post("/api/servicios"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"duracionMinutos\":-5,\"precio\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.duracionMinutos").exists());
    }

    @Test
    @DisplayName("Confirmar y pagar: el cliente paga su reserva una sola vez")
    void confirmarYPagar() throws Exception {
        Reserva r = guardarReserva(empleado, sala, manana10, EstadoReserva.PENDIENTE);
        String pago = "{\"monto\":120.00,\"metodoPago\":\"TARJETA\"}";
        mockMvc.perform(auth(post("/api/db/reservas/" + r.getId() + "/confirmar-y-pagar"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        mockMvc.perform(auth(post("/api/db/reservas/" + r.getId() + "/confirmar-y-pagar"), tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpect(status().isBadRequest());
    }

    // ---------- Agenda del especialista (Tarea 8) ----------

    @Test
    @DisplayName("Un ESPECIALISTA vinculado a su empleado consulta su agenda")
    void agendaDelEspecialista() throws Exception {
        mockMvc.perform(auth(post("/api/auth/usuarios"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"esp@test.com\",\"password\":\"Especialista1\",\"rol\":\"ESPECIALISTA\",\"empleadoId\":"
                                + empleado.getId() + "}"))
                .andExpect(status().isCreated());
        guardarReserva(empleado, sala, manana10, EstadoReserva.CONFIRMADA);
        guardarReserva(empleado2, sala2, manana10, EstadoReserva.CONFIRMADA);

        String tokenEsp = jwtService.generarToken(userDetailsService.loadUserByUsername("esp@test.com"));
        mockMvc.perform(auth(get("/api/db/reservas/agenda"), tokenEsp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].empleadoId").value(empleado.getId()));
        mockMvc.perform(auth(get("/api/db/reservas/agenda"), tokenCliente)).andExpect(status().isForbidden());
        assertThat(empleadoRepository.findById(empleado.getId()).orElseThrow().getUsuarioId()).isNotNull();
    }

    // ---------- utilidades ----------

    private String token(String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode("Clave123"));
        u.setRol(rol);
        usuarioRepository.save(u);
        return jwtService.generarToken(userDetailsService.loadUserByUsername(correo));
    }

    private Empleado guardarEmpleado(String nombre) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        return empleadoRepository.save(e);
    }

    private Sala guardarSala(String nombre) {
        Sala s = new Sala();
        s.setNombre(nombre);
        s.setCapacidad(1);
        return salaRepository.save(s);
    }

    private Reserva guardarReserva(Empleado e, Sala s, LocalDateTime inicio, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setEmpleado(e);
        r.setSala(s);
        r.setServicio(servicio);
        r.setFechaHora(inicio);
        r.setEstado(estado);
        return reservaRepository.save(r);
    }

    private org.springframework.test.web.servlet.ResultActions crearReserva(String token, Empleado e, Sala s,
                                                                             LocalDateTime inicio) throws Exception {
        return mockMvc.perform(auth(post("/api/db/reservas"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(e, s, inicio, null)));
    }

    private String json(Empleado e, Sala s, LocalDateTime inicio, String extra) {
        return "{\"empleadoId\":" + e.getId() + ",\"servicioId\":" + servicio.getId() + ",\"salaId\":" + s.getId()
                + ",\"fechaHora\":\"" + fmt(inicio) + "\"" + (extra != null ? "," + extra : "") + "}";
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private static String fmt(LocalDateTime t) {
        return t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
