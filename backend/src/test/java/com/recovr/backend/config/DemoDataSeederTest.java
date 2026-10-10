package com.recovr.backend.config;

import com.recovr.backend.dto.ReservaResponse;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "recovr.seed.demo-data=true",
        "recovr.seed.demo-password=Demo12345!",
        "recovr.seed.inicio=2026-10-01",
        "recovr.seed.fin=2026-12-31",
        "spring.datasource.url=jdbc:h2:mem:recovr_seedtest;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class DemoDataSeederTest {

    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("El seed cubre octubre a diciembre con todos los estados y pagos coherentes")
    void cubreTresMesesConEstadosYPagos() {
        List<Reserva> reservas = reservaRepository.findAll();

        assertThat(reservas).hasSizeGreaterThan(300);
        assertThat(reservas).allSatisfy(r -> assertThat(r.getFechaHora().toLocalDate())
                .isBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)));
        assertThat(reservas.stream().map(r -> r.getFechaHora().getMonthValue()).distinct()).containsExactlyInAnyOrder(10, 11, 12);
        assertThat(reservas.stream().map(Reserva::getEstado).distinct())
                .containsExactlyInAnyOrder(EstadoReserva.values());

        long conPago = reservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA || r.getEstado() == EstadoReserva.COMPLETADA).count();
        assertThat(pagoRepository.count()).isEqualTo(conPago);
        assertThat(usuarioRepository.existsByCorreo("admin.demo@recovr.test")).isTrue();
    }

    @Test
    @DisplayName("El seed respeta la regla de negocio: sin cruces de cabina ni de terapeuta")
    void noHayCrucesDeAgenda() {
        List<Reserva> activas = reservaRepository.findAll().stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA).toList();

        for (int i = 0; i < activas.size(); i++) {
            Reserva a = activas.get(i);
            for (int j = i + 1; j < activas.size(); j++) {
                Reserva b = activas.get(j);
                boolean mismoRecurso = a.getSala().getId().equals(b.getSala().getId())
                        || a.getEmpleado().getId().equals(b.getEmpleado().getId());
                if (!mismoRecurso) {
                    continue;
                }
                boolean cruce = a.getFechaHora().isBefore(ReservaResponse.finBloque(b.getFechaHora(), b.getServicio()))
                        && b.getFechaHora().isBefore(ReservaResponse.finBloque(a.getFechaHora(), a.getServicio()));
                assertThat(cruce).as("cruce entre reservas %d y %d", a.getId(), b.getId()).isFalse();
            }
        }
    }
}
