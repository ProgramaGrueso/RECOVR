package com.recovr.backend;

import com.recovr.backend.config.RelojConfig;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug: al reprogramar sin indicar cabina (salaId nulo, "asignación automática" en el panel) se conservaba siempre la
 * cabina anterior. Si esa cabina estaba ocupada en el nuevo horario respondía 409 aunque hubiera otra libre, cuando
 * la API documenta que sin salaId se asigna la primera cabina libre.
 */
@SpringBootTest
@Transactional
class ReprogramarReservaTest {

    @Autowired private ReservaService reservaService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private SalaRepository salaRepository;

    private Cliente ana;
    private Cliente beto;
    private Empleado ely;
    private Empleado pamela;
    private Servicio servicio;
    private Sala cabina1;
    private Sala cabina2;
    private LocalDateTime dia;

    @BeforeEach
    void setUp() {
        dia = LocalDateTime.now(RelojConfig.ZONA_NEGOCIO).plusDays(3).withHour(15).withMinute(0).withSecond(0).withNano(0);
        ana = cliente("ana.reprog@example.com");
        beto = cliente("beto.reprog@example.com");
        ely = empleado("Ely Reprog");
        pamela = empleado("Pamela Reprog");
        servicio = new Servicio();
        servicio.setNombre("Relajante");
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);
        servicio.setPrecio(new BigDecimal("180.00"));
        servicio = servicioRepository.save(servicio);
        cabina1 = sala("Cabina A");
        cabina2 = sala("Cabina B");
    }

    @Test
    @DisplayName("Reprogramar sin cabina a un horario donde la cabina actual está ocupada usa otra cabina libre")
    void reprogramarSinCabinaBuscaOtraLibre() {
        Reserva mia = crear(ana, ely, cabina1, dia);
        crear(beto, pamela, cabina1, dia.plusHours(3)); // la cabina A queda ocupada a las 18:00

        Reserva movida = reservaService.actualizar(mia.getId(),
                new ReservaRequest(null, ely.getId(), servicio.getId(), null, dia.plusHours(3), null));

        assertThat(movida.getFechaHora()).isEqualTo(dia.plusHours(3));
        assertThat(movida.getSala().getId()).isEqualTo(cabina2.getId());
    }

    @Test
    @DisplayName("Reprogramar sin cabina conserva la cabina actual si sigue libre en el nuevo horario")
    void reprogramarSinCabinaConservaLaActualSiEstaLibre() {
        Reserva mia = crear(ana, ely, cabina2, dia);

        Reserva movida = reservaService.actualizar(mia.getId(),
                new ReservaRequest(null, ely.getId(), servicio.getId(), null, dia.plusHours(2), null));

        assertThat(movida.getSala().getId()).isEqualTo(cabina2.getId());
    }

    private Reserva crear(Cliente c, Empleado e, Sala s, LocalDateTime cuando) {
        return reservaService.crearDesdeRequest(
                new ReservaRequest(c.getId(), e.getId(), servicio.getId(), s.getId(), cuando, null), "recepcion@example.com", true);
    }

    private Cliente cliente(String correo) {
        Cliente c = new Cliente();
        c.setNombre(correo);
        c.setCorreo(correo);
        return clienteRepository.save(c);
    }

    private Empleado empleado(String nombre) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        return empleadoRepository.save(e);
    }

    private Sala sala(String nombre) {
        Sala s = new Sala();
        s.setNombre(nombre);
        return salaRepository.save(s);
    }
}
