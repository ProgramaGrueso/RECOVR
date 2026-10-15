package com.recovr.backend;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.security.CustomUserDetailsService;
import com.recovr.backend.security.JwtService;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.service.EmpleadoService;
import com.recovr.backend.service.OperacionService;
import com.recovr.backend.service.ReservaCicloVidaService;
import com.recovr.backend.service.ReservaService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida de la reserva: expiración, no asistencia, completado automático, check-in, finalizar,
 * borrado, y estado en vivo de cabinas y terapeutas. Todas con una hora "ahora" fija para que sean deterministas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservaCicloVidaTest {

    @Autowired private ReservaCicloVidaService cicloVida;
    @Autowired private ReservaService reservaService;
    @Autowired private OperacionService operacion;
    @Autowired private EmpleadoService empleadoService;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private MockMvc mockMvc;

    private Cliente cliente;
    private Empleado ely;
    private Sala cabina;
    private Servicio servicio;   // 60 min de sesión + 15 de limpieza
    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 15, 16, 0);

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNombre("Cliente Ciclo");
        cliente.setCorreo("ciclo@test.com");
        cliente = clienteRepository.save(cliente);
        ely = new Empleado();
        ely.setNombre("Ely");
        ely = empleadoRepository.save(ely);
        cabina = new Sala();
        cabina.setNombre("Cabina 01");
        cabina.setCapacidad(2);
        cabina = salaRepository.save(cabina);
        servicio = new Servicio();
        servicio.setNombre("RC-01 Masaje");
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);
        servicio.setPrecio(new BigDecimal("200.00"));
        servicio = servicioRepository.save(servicio);
    }

    @Test
    @DisplayName("PENDIENTE sin confirmar expira (CANCELADA) pasada la tolerancia, no antes")
    void pendienteExpira() {
        Reserva r = reserva(EstadoReserva.PENDIENTE, null);
        assertThat(cicloVida.actualizarEstados(inicio.plusMinutes(14))).isZero();
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);

        assertThat(cicloVida.actualizarEstados(inicio.plusMinutes(15))).isEqualTo(1);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(r.getObservacion()).contains("Expirada");
    }

    @Test
    @DisplayName("Una cita antigua sin atender (hace meses) también se limpia, y es idempotente")
    void citasAntiguasSeLimpian() {
        Reserva vieja = reserva(EstadoReserva.PENDIENTE, null);
        vieja.setFechaHora(inicio.minusMonths(2));
        reservaRepository.save(vieja);
        assertThat(cicloVida.actualizarEstados(inicio)).isEqualTo(1);
        assertThat(vieja.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(cicloVida.actualizarEstados(inicio)).isZero();
    }

    @Test
    @DisplayName("CONFIRMADA sin check-in pasa a CANCELADA por no asistir; con check-in se COMPLETA al terminar la sesión")
    void noShowYCompletadoAutomatico() {
        Reserva noLlego = reserva(EstadoReserva.CONFIRMADA, null);
        assertThat(cicloVida.actualizarEstados(inicio.plusMinutes(29))).isZero();
        cicloVida.actualizarEstados(inicio.plusMinutes(30));
        assertThat(noLlego.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(noLlego.getObservacion()).contains("No asistió");

        Reserva llego = reserva(EstadoReserva.CONFIRMADA, inicio.minusMinutes(5));
        llego.setFechaHora(inicio.plusDays(1));
        llego.setCheckIn(inicio.plusDays(1));
        reservaRepository.save(llego);
        assertThat(cicloVida.actualizarEstados(inicio.plusDays(1).plusMinutes(59))).isZero();
        cicloVida.actualizarEstados(inicio.plusDays(1).plusMinutes(60));
        assertThat(llego.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);
    }

    @Test
    @DisplayName("Check-in: solo CONFIRMADA, desde 30 min antes, una vez y antes de que termine la sesión")
    void reglasDeCheckIn() {
        Reserva pendiente = reserva(EstadoReserva.PENDIENTE, null);
        assertThatThrownBy(() -> reservaService.checkIn(pendiente.getId(), inicio))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CONFIRMADA");

        Reserva r = reserva(EstadoReserva.CONFIRMADA, null);
        assertThatThrownBy(() -> reservaService.checkIn(r.getId(), inicio.minusMinutes(31)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("pronto");
        assertThat(reservaService.checkIn(r.getId(), inicio.minusMinutes(30)).getCheckIn()).isEqualTo(inicio.minusMinutes(30));
        assertThatThrownBy(() -> reservaService.checkIn(r.getId(), inicio))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ya tiene check-in");
    }

    @Test
    @DisplayName("Finalizar exige check-in; y COMPLETADA/CANCELADA no se editan ni cambian de estado")
    void finalizarYEstadosFinales() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, null);
        assertThatThrownBy(() -> reservaService.finalizar(r.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("check-in");
        r.setCheckIn(inicio);
        reservaRepository.save(r);
        assertThat(reservaService.finalizar(r.getId()).getEstado()).isEqualTo(EstadoReserva.COMPLETADA);
        assertThatThrownBy(() -> reservaService.cancelar(r.getId(), "x", true))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> reservaService.eliminar(r.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CANCELADAS");
    }

    @Test
    @DisplayName("Estado de la cabina: DISPONIBLE → ESPERANDO_CLIENTE → OCUPADA → EN_SANITIZACION → DISPONIBLE")
    void estadoDeCabina() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, null);
        assertThat(estadoCabina(inicio.minusMinutes(10))).isEqualTo("DISPONIBLE");
        assertThat(estadoCabina(inicio.plusMinutes(5))).isEqualTo("ESPERANDO_CLIENTE");
        r.setCheckIn(inicio.plusMinutes(2));
        reservaRepository.save(r);
        assertThat(estadoCabina(inicio.plusMinutes(30))).isEqualTo("OCUPADA");
        r.setEstado(EstadoReserva.COMPLETADA);
        reservaRepository.save(r);
        assertThat(estadoCabina(inicio.plusMinutes(65))).isEqualTo("EN_SANITIZACION");
        assertThat(estadoCabina(inicio.plusMinutes(75))).isEqualTo("DISPONIBLE");
    }

    @Test
    @DisplayName("Estado de la terapeuta: LIBRE, POR_INICIAR y EN_SESION")
    void estadoDeTerapeuta() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, null);
        assertThat(operacion.estadoEmpleados(inicio.minusHours(1)).get(0).estado()).isEqualTo("LIBRE");
        assertThat(operacion.estadoEmpleados(inicio.minusHours(1)).get(0).proximaReserva()).isEqualTo(inicio);
        assertThat(operacion.estadoEmpleados(inicio.plusMinutes(5)).get(0).estado()).isEqualTo("POR_INICIAR");
        r.setCheckIn(inicio);
        reservaRepository.save(r);
        assertThat(operacion.estadoEmpleados(inicio.plusMinutes(5)).get(0).estado()).isEqualTo("EN_SESION");
    }

    @Test
    @DisplayName("KPI: ingresos del mes sin canceladas, ocupación de hoy y tasa de cancelación")
    void resumenDelPanel() {
        Reserva completada = reserva(EstadoReserva.COMPLETADA, inicio);
        pago(completada, "200.00", inicio.minusHours(1));
        Reserva cancelada = reserva(EstadoReserva.CANCELADA, null);
        cancelada.setFechaHora(inicio.plusHours(3));
        reservaRepository.save(cancelada);
        pago(cancelada, "999.00", inicio.minusHours(1));

        var resumen = operacion.resumen(inicio.plusHours(6));
        assertThat(resumen.ingresosMes()).isEqualByComparingTo("200.00");
        assertThat(resumen.sesionesCompletadasMes()).isEqualTo(1);
        assertThat(resumen.canceladasMes()).isEqualTo(1);
        assertThat(resumen.tasaCancelacionPct()).isEqualTo(50.0);
        // 60 min reservados / (1 cabina × 660 min de atención)
        assertThat(resumen.ocupacionHoyPct()).isEqualTo(9.1);
    }

    @Test
    @DisplayName("/api/operacion solo lo ven ADMIN y RECEPCIONISTA")
    void seguridadDeOperacion() throws Exception {
        String recepcion = token("rec.ciclo@test.com", Rol.RECEPCIONISTA);
        String clienteToken = token("cli.ciclo@test.com", Rol.CLIENTE);
        mockMvc.perform(get("/api/operacion/salas").header("Authorization", "Bearer " + recepcion))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Cabina 01"));
        mockMvc.perform(get("/api/operacion/resumen").header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/operacion/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Baja lógica de una terapeuta: bloqueada con reservas activas; luego sale del catálogo y no recibe reservas")
    void bajaLogicaDeTerapeuta() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, null);
        assertThatThrownBy(() -> empleadoService.cambiarActivo(ely.getId(), false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("reservas activas");

        r.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(r);
        assertThat(empleadoService.cambiarActivo(ely.getId(), false).isActivo()).isFalse();
        assertThat(empleadoService.listarActivos()).doesNotContain(ely);
        assertThat(operacion.estadoEmpleados(inicio).get(0).activo()).isFalse();

        var futura = new ReservaRequest(cliente.getId(), ely.getId(), servicio.getId(), cabina.getId(),
                LocalDateTime.now().plusDays(3), null);
        assertThatThrownBy(() -> reservaService.crearDesdeRequest(futura, "x", true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("de baja");

        assertThat(empleadoService.cambiarActivo(ely.getId(), true).isActivo()).isTrue();
        assertThat(empleadoService.listarActivos()).contains(ely);
    }

    private String estadoCabina(LocalDateTime ahora) {
        return operacion.estadoSalas(ahora).get(0).estado();
    }

    private Reserva reserva(EstadoReserva estado, LocalDateTime checkIn) {
        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setEmpleado(ely);
        r.setSala(cabina);
        r.setServicio(servicio);
        r.setFechaHora(inicio);
        r.setEstado(estado);
        r.setCheckIn(checkIn);
        return reservaRepository.save(r);
    }

    private void pago(Reserva r, String monto, LocalDateTime fecha) {
        Pago p = new Pago();
        p.setReserva(r);
        p.setMonto(new BigDecimal(monto));
        p.setMetodoPago("Yape");
        p.setFechaPago(fecha);
        pagoRepository.save(p);
    }

    private String token(String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode("Clave123"));
        u.setRol(rol);
        usuarioRepository.save(u);
        return jwtService.generarToken(userDetailsService.loadUserByUsername(correo));
    }
}
