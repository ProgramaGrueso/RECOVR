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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los listados del panel son paginados y el de reservas se puede limitar a una ventana de fechas. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListadosPaginadosTest {

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

    private String token;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario();
        u.setCorreo("admin.listados@example.com");
        u.setPassword(passwordEncoder.encode("Clave1234"));
        u.setRol(Rol.ADMIN);
        usuarioRepository.save(u);
        token = "Bearer " + jwtService.generarToken(userDetailsService.loadUserByUsername(u.getCorreo()));

        Empleado e = new Empleado();
        e.setNombre("Terapeuta Listado");
        empleadoRepository.save(e);
        Servicio s = new Servicio();
        s.setNombre("Servicio Listado");
        s.setDuracionMinutos(60);
        s.setPrecio(new BigDecimal("100"));
        servicioRepository.save(s);
        Sala sala = new Sala();
        sala.setNombre("Sala Listado");
        salaRepository.save(sala);
        for (int i = 0; i < 3; i++) {
            Cliente c = new Cliente();
            c.setNombre("Cliente " + (char) ('A' + i));
            c.setCorreo("listado" + i + "@example.com");
            clienteRepository.save(c);
            Reserva r = new Reserva();
            r.setCliente(c);
            r.setEmpleado(e);
            r.setServicio(s);
            r.setSala(sala);
            r.setFechaHora(LocalDateTime.of(2027, 3, 10 + i, 15, 0)); // 10, 11 y 12 de marzo
            r.setEstado(EstadoReserva.CONFIRMADA);
            reservaRepository.save(r);
        }
    }

    @Test
    @DisplayName("Reservas: la ventana [desde, hasta) filtra por día y las cabeceras traen el total")
    void reservasPorVentana() throws Exception {
        mockMvc.perform(get("/api/db/reservas").param("desde", "2027-03-11").param("hasta", "2027-03-13").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fechaHora").value("2027-03-12T15:00:00"))
                .andExpect(header().string("X-Total-Count", "2"));

        mockMvc.perform(get("/api/db/reservas").param("desde", "2027-03-10").param("tamano", "1").param("pagina", "1").header("Authorization", token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fechaHora").value("2027-03-11T15:00:00"))
                .andExpect(header().string("X-Total-Pages", "3"));
    }

    @Test
    @DisplayName("Reservas: una ventana con 'desde' posterior a 'hasta' es 400")
    void ventanaInvalida() throws Exception {
        mockMvc.perform(get("/api/db/reservas").param("desde", "2027-03-12").param("hasta", "2027-03-10").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Clientes: paginados por nombre con el total en la cabecera")
    void clientesPaginados() throws Exception {
        mockMvc.perform(get("/api/clientes").param("tamano", "2").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Cliente A"))
                .andExpect(header().string("X-Total-Count", "3"));
    }
}
