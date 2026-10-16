package com.recovr.backend;

import com.recovr.backend.entity.*;
import com.recovr.backend.repository.*;
import com.recovr.backend.security.JwtService;
import com.recovr.backend.security.MatrizPermisos;
import com.recovr.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seguridad por rol de punta a punta: la matriz documentada coincide con la API real, sesión y perfil,
 * política de contraseñas, bloqueo por intentos, CORS, auditoría, fichas confidenciales y portal del cliente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadRolesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private com.recovr.backend.repository.PagoRepository pagoRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;

    private Servicio servicio;
    private Sala sala;

    @BeforeEach
    void setUp() {
        servicio = new Servicio();
        servicio.setNombre("Servicio Seg");
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);
        servicio.setPrecio(new BigDecimal("250.00"));
        servicio = servicioRepository.save(servicio);
        sala = new Sala();
        sala.setNombre("Cabina Seg " + UUID.randomUUID());
        sala.setCapacidad(2);
        sala = salaRepository.save(sala);
    }

    // ---------------------------------------------------------------- matriz

    @Test
    @DisplayName("La matriz de permisos documentada coincide con lo que la API realmente permite, rol por rol")
    void matrizCoincideConLaApi() throws Exception {
        Map<Rol, String> tokens = Map.of(
                Rol.ADMIN, token(crearUsuario(Rol.ADMIN)), Rol.RECEPCIONISTA, token(crearUsuario(Rol.RECEPCIONISTA)),
                Rol.ESPECIALISTA, token(crearUsuario(Rol.ESPECIALISTA)), Rol.CLIENTE, token(crearUsuario(Rol.CLIENTE)));
        for (MatrizPermisos.Permiso p : MatrizPermisos.todos()) {
            mockMvc.perform(peticion(p.metodo(), p.ruta(), null)).andExpect(status().isUnauthorized());
            for (Rol rol : Rol.values()) {
                int status = mockMvc.perform(peticion(p.metodo(), p.ruta(), tokens.get(rol))).andReturn().getResponse().getStatus();
                String msg = p.metodo() + " " + p.ruta() + " como " + rol + " → " + status;
                if (p.roles().contains(rol)) {
                    assertThat(status).as(msg).isNotIn(401, 403).isLessThan(500);
                } else {
                    assertThat(status).as(msg).isEqualTo(403);
                }
            }
        }
    }

    // ---------------------------------------------------------------- sesión

    @Test
    @DisplayName("/auth/me devuelve el perfil sin datos sensibles y exige token")
    void perfilDeLaSesion() throws Exception {
        Usuario u = crearUsuario(Rol.ADMIN);
        u.setNombre("Mateo Salazar");
        usuarioRepository.save(u);
        MvcResult r = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.nombre").value("Mateo Salazar"))
                .andReturn();
        assertThat(r.getResponse().getContentAsString()).doesNotContainIgnoringCase("password").doesNotContain("$2a$");
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token.invalido.xxx")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Política de contraseñas: mínimo 8, con letras y números")
    void politicaDeContrasenas() throws Exception {
        assertThat(registro("pw1-" + UUID.randomUUID() + "@t.com", "12345678").getResponse().getStatus()).isEqualTo(400);
        assertThat(registro("pw2-" + UUID.randomUUID() + "@t.com", "abcdefgh").getResponse().getStatus()).isEqualTo(400);
        assertThat(registro("pw3-" + UUID.randomUUID() + "@t.com", "Ab1").getResponse().getStatus()).isEqualTo(400);
        MvcResult ok = registro("pw4-" + UUID.randomUUID() + "@t.com", "Clave1234");
        assertThat(ok.getResponse().getStatus()).isEqualTo(201);
        assertThat(ok.getResponse().getContentAsString()).contains("\"rol\":\"CLIENTE\"").contains("\"nombre\":\"Cliente Nuevo\"");
    }

    @Test
    @DisplayName("Tras 5 intentos fallidos la cuenta se bloquea (429) aunque luego llegue la contraseña correcta")
    void bloqueoPorIntentos() throws Exception {
        String correo = "bloq-" + UUID.randomUUID() + "@t.com";
        assertThat(registro(correo, "Clave1234").getResponse().getStatus()).isEqualTo(201);
        for (int i = 0; i < 5; i++) {
            login(correo, "incorrecta" + i).andExpect(status().isUnauthorized());
        }
        login(correo, "Clave1234").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("Demasiados intentos")));
        // otro correo no se ve afectado
        String otro = "libre-" + UUID.randomUUID() + "@t.com";
        registro(otro, "Clave1234");
        login(otro, "Clave1234").andExpect(status().isOk());
    }

    @Test
    @DisplayName("El error de login no revela si el correo existe")
    void mensajeUniformeDeLogin() throws Exception {
        String existente = "uni-" + UUID.randomUUID() + "@t.com";
        registro(existente, "Clave1234");
        String m1 = login(existente, "mala").andReturn().getResponse().getContentAsString();
        String m2 = login("no-existe-" + UUID.randomUUID() + "@t.com", "mala").andReturn().getResponse().getContentAsString();
        assertThat(m1.replaceAll("\"timestamp\":\"[^\"]*\",?", "")).isEqualTo(m2.replaceAll("\"timestamp\":\"[^\"]*\",?", ""));
    }

    @Test
    @DisplayName("CORS: solo se aceptan orígenes conocidos (localhost y Vercel), nunca uno cualquiera")
    void corsRestringido() throws Exception {
        mockMvc.perform(options("/api/servicios").header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mockMvc.perform(options("/api/servicios").header("Origin", "https://recovr-demo.vercel.app").header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://recovr-demo.vercel.app"));
        mockMvc.perform(options("/api/servicios").header("Origin", "https://sitio-malicioso.com").header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mockMvc.perform(options("/api/servicios").header("Origin", "https://vercel.app.evil.com").header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ---------------------------------------------------------------- auditoría

    @Test
    @DisplayName("La auditoría registra accesos denegados, cambios y logins fallidos, sin contraseñas")
    void auditoria() throws Exception {
        Usuario cliente = crearUsuario(Rol.CLIENTE);
        Usuario admin = crearUsuario(Rol.ADMIN);
        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + token(cliente))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/salas").header("Authorization", "Bearer " + token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Cabina Auditada\",\"capacidad\":2}")).andExpect(status().is2xxSuccessful());
        login("auditoria-" + UUID.randomUUID() + "@t.com", "SecretoQueNoDebeVerse1").andExpect(status().isUnauthorized());

        String cuerpo = mockMvc.perform(get("/api/sistema/auditoria?limite=50").header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"estado\":403").contains(cliente.getCorreo()).contains("/api/clientes")
                .contains("\"accion\":\"POST\"").contains("/api/salas").contains("LOGIN_FALLIDO")
                .doesNotContain("SecretoQueNoDebeVerse1").doesNotContain("Cabina Auditada");
    }

    // ---------------------------------------------------------------- fichas

    @Test
    @DisplayName("Fichas: la terapeuta solo accede a clientes que atiende; nadie más puede leerlas")
    void fichasConfidenciales() throws Exception {
        Usuario uEly = crearUsuario(Rol.ESPECIALISTA);
        Usuario uOtra = crearUsuario(Rol.ESPECIALISTA);
        Empleado ely = empleado("Ely Seg", uEly);
        Empleado otra = empleado("Otra Seg", uOtra);
        Cliente suyo = cliente("Cliente de Ely", null);
        Cliente ajeno = cliente("Cliente Ajeno", null);
        reserva(suyo, ely, LocalDateTime.now().plusDays(2), EstadoReserva.CONFIRMADA);
        reserva(ajeno, otra, LocalDateTime.now().plusDays(3), EstadoReserva.CONFIRMADA);
        String t = "Bearer " + token(uEly);

        mockMvc.perform(get("/api/fichas/mis-clientes").header("Authorization", t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nombre").value("Cliente de Ely"));
        mockMvc.perform(get("/api/fichas/cliente/" + suyo.getId()).header("Authorization", t)).andExpect(status().isOk());
        mockMvc.perform(get("/api/fichas/cliente/" + ajeno.getId()).header("Authorization", t)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/fichas/cliente/" + ajeno.getId()).header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"alergias\":\"x\"}")).andExpect(status().isForbidden());

        mockMvc.perform(put("/api/fichas/cliente/" + suyo.getId()).header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alergias\":\"Lavanda\",\"presionPreferida\":\"MEDIA\",\"zonasRestringidas\":\"Cervical\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alergias").value("Lavanda")).andExpect(jsonPath("$.actualizadoPor").value("Ely Seg"));
        mockMvc.perform(post("/api/fichas/cliente/" + suyo.getId() + "/notas").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tratamiento\":\"Relajante\",\"observaciones\":\"Buena tolerancia\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.notasSesion[0].propia").value(true));

        // otra terapeuta no puede borrar una nota ajena
        Cliente compartido = cliente("Cliente Compartido", null);
        reserva(compartido, ely, LocalDateTime.now().plusDays(4), EstadoReserva.CONFIRMADA);
        reserva(compartido, otra, LocalDateTime.now().plusDays(5), EstadoReserva.CONFIRMADA);
        String creada = mockMvc.perform(post("/api/fichas/cliente/" + compartido.getId() + "/notas").header("Authorization", t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tratamiento\":\"Nota de Ely\"}")).andReturn().getResponse().getContentAsString();
        long notaId = Long.parseLong(creada.replaceAll(".*\"notasSesion\":\\[\\{\"id\":(\\d+).*", "$1"));
        mockMvc.perform(delete("/api/fichas/notas/" + notaId).header("Authorization", "Bearer " + token(uOtra))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/fichas/notas/" + notaId).header("Authorization", t)).andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------- portal del cliente

    @Test
    @DisplayName("Mi cuenta: puntos y nivel salen de las sesiones completadas, no se pueden inventar")
    void puntosYNivel() throws Exception {
        Usuario u = crearUsuario(Rol.CLIENTE);
        Cliente c = cliente("Cliente Puntos", u);
        Empleado e = empleado("Terapeuta Pts", null);
        for (int i = 0; i < 3; i++) {
            reserva(c, e, LocalDateTime.now().minusDays(10 + i), EstadoReserva.COMPLETADA); // 3 × S/ 250
        }
        reserva(c, e, LocalDateTime.now().minusDays(20), EstadoReserva.CANCELADA);          // no suma
        mockMvc.perform(get("/api/mi-cuenta/resumen").header("Authorization", "Bearer " + token(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntos").value(750)).andExpect(jsonPath("$.nivel").value("Plata"))
                .andExpect(jsonPath("$.siguienteNivel").value("Oro")).andExpect(jsonPath("$.puntosParaSiguiente").value(750))
                .andExpect(jsonPath("$.sesionesCompletadas").value(3));
    }

    @Test
    @DisplayName("Cancelación del cliente: 4 h de anticipación y máximo 2 propias al mes; el personal cancela siempre")
    void politicaDeCancelacion() throws Exception {
        Usuario u = crearUsuario(Rol.CLIENTE);
        Cliente c = cliente("Cliente Cancela", u);
        Empleado e = empleado("Terapeuta Cancela", null);
        String tc = "Bearer " + token(u);

        Reserva pronto = reserva(c, e, LocalDateTime.now().plusHours(2), EstadoReserva.CONFIRMADA);
        mockMvc.perform(patch("/api/db/reservas/" + pronto.getId() + "/cancelar").header("Authorization", tc))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("menos de 4 horas")));
        mockMvc.perform(patch("/api/db/reservas/" + pronto.getId() + "/cancelar")
                .header("Authorization", "Bearer " + token(crearUsuario(Rol.RECEPCIONISTA)))).andExpect(status().isOk());

        for (int i = 0; i < 2; i++) {
            Reserva r = reserva(c, e, LocalDateTime.now().plusDays(2 + i), EstadoReserva.PENDIENTE);
            mockMvc.perform(patch("/api/db/reservas/" + r.getId() + "/cancelar").header("Authorization", tc))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.observacion").value("Cancelada por el cliente."));
        }
        Reserva tercera = reserva(c, e, LocalDateTime.now().plusDays(6), EstadoReserva.PENDIENTE);
        mockMvc.perform(patch("/api/db/reservas/" + tercera.getId() + "/cancelar").header("Authorization", tc))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("límite")));
        mockMvc.perform(get("/api/mi-cuenta/resumen").header("Authorization", tc))
                .andExpect(jsonPath("$.cancelacionesMes").value(2)).andExpect(jsonPath("$.limiteCancelacionesMes").value(2));
    }

    // ---------------------------------------------------------------- utilidades

    private MockHttpServletRequestBuilder peticion(String metodo, String ruta, String token) {
        MockHttpServletRequestBuilder b = request(org.springframework.http.HttpMethod.valueOf(metodo), ruta)
                .contentType(MediaType.APPLICATION_JSON).content("{}");
        return token != null ? b.header("Authorization", "Bearer " + token) : b;
    }

    private MvcResult registro(String correo, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Cliente Nuevo\",\"correo\":\"" + correo + "\",\"password\":\"" + password + "\",\"telefono\":\"999111222\"}")).andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions login(String correo, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"));
    }

    private Usuario crearUsuario(Rol rol) {
        Usuario u = new Usuario();
        u.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@seg.test");
        u.setPassword(passwordEncoder.encode("Clave1234"));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    private String token(Usuario u) {
        return jwtService.generarToken(userDetailsService.loadUserByUsername(u.getCorreo()));
    }

    private Empleado empleado(String nombre, Usuario u) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        if (u != null) {
            e.setUsuarioId(u.getId());
        }
        return empleadoRepository.save(e);
    }

    private Cliente cliente(String nombre, Usuario u) {
        Cliente c = new Cliente();
        c.setNombre(nombre);
        c.setCorreo(UUID.randomUUID() + "@cli.test");
        if (u != null) {
            c.setUsuarioId(u.getId());
        }
        return clienteRepository.save(c);
    }

    private Reserva reserva(Cliente c, Empleado e, LocalDateTime inicio, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setCliente(c);
        r.setEmpleado(e);
        r.setServicio(servicio);
        r.setSala(sala);
        r.setFechaHora(inicio);
        r.setEstado(estado);
        r = reservaRepository.save(r);
        if (estado == EstadoReserva.COMPLETADA) {
            // Los puntos salen de lo realmente pagado: una sesión completada sin pago no suma
            com.recovr.backend.entity.Pago pago = new com.recovr.backend.entity.Pago();
            pago.setReserva(r);
            pago.setMonto(servicio.getPrecio());
            pago.setMetodoPago("EFECTIVO");
            pago.setFechaPago(inicio);
            r.setPago(pagoRepository.save(pago));
        }
        return r;
    }
}
