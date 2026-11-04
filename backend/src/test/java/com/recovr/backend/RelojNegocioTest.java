package com.recovr.backend;

import com.recovr.backend.config.RelojConfig;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.dto.TurnoDisponibleResponse;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.service.ReservaCicloVidaService;
import com.recovr.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bug: las reglas que dependen de "ahora" usaban {@code LocalDateTime.now()}, es decir, la zona horaria de la JVM.
 * Las pruebas (y cualquier contenedor con TZ=UTC) corren 5 horas por delante de Lima, así que de noche una sesión
 * de esa misma noche se veía como "pasada". Ahora todo usa el reloj del negocio ({@link RelojConfig}, America/Lima).
 * Aquí el reloj se fija el 15/01/2030 a las 20:00 de Lima: el servicio debe razonar con esa hora, no con la del sistema.
 */
@SpringBootTest
@Transactional
class RelojNegocioTest {

    static final LocalDateTime AHORA_LIMA = LocalDateTime.of(2030, 1, 15, 20, 0);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(AHORA_LIMA.atZone(RelojConfig.ZONA_NEGOCIO).toInstant(), RelojConfig.ZONA_NEGOCIO);
        }
    }

    @Autowired private ReservaService reservaService;
    @Autowired private com.recovr.backend.service.DisponibilidadService disponibilidadService;
    @Autowired private ReservaCicloVidaService cicloVidaService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ReservaRepository reservaRepository;

    private Cliente cliente;
    private Empleado terapeuta;
    private Servicio servicio;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNombre("Paciente Reloj");
        cliente.setCorreo("reloj@example.com");
        cliente = clienteRepository.save(cliente);
        terapeuta = new Empleado();
        terapeuta.setNombre("Terapeuta Reloj");
        terapeuta = empleadoRepository.save(terapeuta);
        servicio = new Servicio();
        servicio.setNombre("Relajante");
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);
        servicio.setPrecio(new BigDecimal("120.00"));
        servicio = servicioRepository.save(servicio);
        Sala sala = new Sala();
        sala.setNombre("Cabina Reloj");
        salaRepository.save(sala);
    }

    @Test
    @DisplayName("Una sesión anterior a la hora de Lima se rechaza como pasada aunque el reloj del sistema diga otra cosa")
    void rechazaHorarioPasadoSegunLima() {
        ReservaRequest pasada = new ReservaRequest(cliente.getId(), terapeuta.getId(), servicio.getId(), null,
                LocalDateTime.of(2030, 1, 15, 18, 0), null);
        assertThatThrownBy(() -> reservaService.crearDesdeRequest(pasada, "recepcion@example.com", true))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("futura");

        ReservaRequest futura = new ReservaRequest(cliente.getId(), terapeuta.getId(), servicio.getId(), null,
                LocalDateTime.of(2030, 1, 15, 21, 0), null);
        Reserva creada = reservaService.crearDesdeRequest(futura, "recepcion@example.com", true);
        assertThat(creada.getCreadaEn()).isEqualTo(AHORA_LIMA);
    }

    @Test
    @DisplayName("La disponibilidad marca como pasados los turnos anteriores a la hora de Lima")
    void disponibilidadSegunLima() {
        List<TurnoDisponibleResponse> turnos = disponibilidadService.disponibilidad(terapeuta.getId(), servicio.getId(),
                LocalDate.of(2030, 1, 15), null);

        assertThat(turnos).filteredOn(t -> t.hora().compareTo("20:00") < 0)
                .allSatisfy(t -> assertThat(t.motivo()).isEqualTo("Este horario ya pasó"));
        assertThat(turnos).filteredOn(t -> t.hora().equals("21:00")).singleElement()
                .satisfies(t -> assertThat(t.disponible()).isTrue());
    }

    @Test
    @DisplayName("El planificador usa la hora de Lima para dar por no asistida una sesión confirmada")
    void planificadorSegunLima() {
        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setEmpleado(terapeuta);
        r.setServicio(servicio);
        r.setSala(salaRepository.findAll().get(0));
        r.setFechaHora(LocalDateTime.of(2030, 1, 15, 19, 0)); // empezó hace 1 h (hora de Lima), sin check-in
        r.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(r);

        cicloVidaService.ejecutarPeriodicamente();

        assertThat(reservaRepository.findById(r.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }
}
