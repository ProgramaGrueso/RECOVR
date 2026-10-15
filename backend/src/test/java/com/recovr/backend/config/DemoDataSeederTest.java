package com.recovr.backend.config;

import com.recovr.backend.dto.ReservaResponse;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.FichaClienteRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "recovr.seed.catalogo=true",
        "recovr.seed.demo-data=true",
        "recovr.seed.inicio=2026-10-01",
        "recovr.seed.fin=2026-12-31",
        "spring.datasource.url=jdbc:h2:mem:recovr_seedtest;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class DemoDataSeederTest {

    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private FichaClienteRepository fichaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

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
                assertThat(cruce).as("cruce entre reservas %d y %d | %s %s %s %s %s | %s %s %s %s %s", a.getId(), b.getId(), a.getFechaHora(), a.getEstado(), a.getSala().getNombre(), a.getEmpleado().getNombre(), a.getServicio().getNombre(), b.getFechaHora(), b.getEstado(), b.getSala().getNombre(), b.getEmpleado().getNombre(), b.getServicio().getNombre()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("Las 11 cuentas demo existen con su rol, nombre y contraseña propia (guardada cifrada) y vinculadas a su ficha")
    void cuentasDemoDocumentadas() {
        assertThat(DemoDataSeeder.CUENTAS).hasSize(11);
        for (DemoDataSeeder.CuentaDemo c : DemoDataSeeder.CUENTAS) {
            var u = usuarioRepository.findByCorreo(c.correo()).orElseThrow();
            assertThat(u.getRol()).isEqualTo(c.rol());
            assertThat(u.getNombre()).isEqualTo(c.nombre());
            assertThat(u.getPassword()).startsWith("$2").isNotEqualTo(c.password());
            assertThat(passwordEncoder.matches(c.password(), u.getPassword())).as(c.usuario()).isTrue();
            if (c.terapeuta() != null) {
                assertThat(empleadoRepository.findByUsuarioId(u.getId())).as(c.usuario()).isPresent()
                        .get().extracting(e -> e.getNombre()).isEqualTo(c.terapeuta());
            }
            if (c.rol() == com.recovr.backend.entity.Rol.CLIENTE) {
                var cli = clienteRepository.findByUsuarioId(u.getId()).orElseThrow();
                assertThat(cli.getNombre()).isEqualTo(c.nombre());
                assertThat(reservaRepository.buscarPorCliente(cli.getId())).hasSizeGreaterThanOrEqualTo(5);
            }
        }
        // Las contraseñas son distintas entre sí
        assertThat(DemoDataSeeder.CUENTAS.stream().map(DemoDataSeeder.CuentaDemo::password).distinct().count()).isEqualTo(11);
        var carlos = clienteRepository.findByCorreo("cliente.demo@recovr.test").orElseThrow();
        assertThat(fichaRepository.findByClienteId(carlos.getId())).isPresent();
    }
}
