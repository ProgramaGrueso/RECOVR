package com.recovr.backend.config;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.FichaClienteRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.service.ReglasReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El seeder demo deja la base como si el sistema llevara 3 meses en uso, con agenda hasta el 31 de diciembre.
 * El "hoy" se fija con un reloj de prueba (4 de noviembre de 2026, 15:00 en Lima) para que el resultado sea estable.
 */
@SpringBootTest(properties = {
        "recovr.seed.catalogo=true",
        "recovr.seed.demo-data=true",
        "spring.datasource.url=jdbc:h2:mem:recovr_seedtest;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
@Transactional
class DemoDataSeederTest {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 15, 0);
    static final LocalDate HOY = AHORA.toLocalDate();

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(AHORA.atZone(RelojConfig.ZONA_NEGOCIO).toInstant(), RelojConfig.ZONA_NEGOCIO);
        }
    }

    /** Contraseña demo aleatoria en cada ejecución: ninguna clave queda escrita en el repositorio. */
    static final String CLAVE_DEMO = "Demo" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "7";

    @org.springframework.test.context.DynamicPropertySource
    static void claveDemo(org.springframework.test.context.DynamicPropertyRegistry registro) {
        registro.add("recovr.seed.demo-password", () -> CLAVE_DEMO);
    }

    @Autowired private DemoDataSeeder seeder;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private FichaClienteRepository fichaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Cubre desde hoy − 90 días hasta el 31 de diciembre, siempre dentro del horario de atención")
    void cubreTresMesesDeHistorialYAgendaHastaFinDeAnio() {
        List<Reserva> reservas = reservaRepository.findAll();

        assertThat(reservas).hasSizeGreaterThan(1000);
        LocalDate primera = reservas.stream().map(r -> r.getFechaHora().toLocalDate()).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate ultima = reservas.stream().map(r -> r.getFechaHora().toLocalDate()).max(Comparator.naturalOrder()).orElseThrow();
        assertThat(primera).isBetween(HOY.minusDays(90), HOY.minusDays(85));
        assertThat(ultima).isBetween(LocalDate.of(2026, 12, 26), LocalDate.of(2026, 12, 31));
        assertThat(reservas).allSatisfy(r -> assertThat(
                ReglasReserva.dentroDeHorario(r.getFechaHora(), ReglasReserva.finSesion(r))).as("#%d %s", r.getId(), r.getFechaHora()).isTrue());
    }

    @Test
    @DisplayName("Pasado: completadas, canceladas y no asistió; futuro: solo pendientes y confirmadas; pagos coherentes")
    void estadosRealistasSegunPasadoOFuturo() {
        List<Reserva> reservas = reservaRepository.findAll();
        List<Reserva> pasadas = reservas.stream().filter(r -> !ReglasReserva.finSesion(r).isAfter(AHORA)).toList();
        List<Reserva> futuras = reservas.stream().filter(r -> r.getFechaHora().isAfter(AHORA)).toList();

        assertThat(pasadas).extracting(Reserva::getEstado).containsOnly(EstadoReserva.COMPLETADA, EstadoReserva.CANCELADA);
        assertThat(pasadas).extracting(Reserva::getObservacion)
                .contains(ReglasReserva.NO_ASISTIO, ReglasReserva.CANCELADA_POR_CLIENTE, ReglasReserva.EXPIRADA_SIN_PAGO);
        double completadas = pasadas.stream().filter(r -> r.getEstado() == EstadoReserva.COMPLETADA).count() / (double) pasadas.size();
        assertThat(completadas).isBetween(0.70, 0.90);

        assertThat(futuras).extracting(Reserva::getEstado).containsOnly(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
        assertThat(futuras).filteredOn(r -> r.getEstado() == EstadoReserva.CONFIRMADA).hasSizeGreaterThan(futuras.size() / 2);

        // Toda sesión cobrada (completada, confirmada o no asistida) tiene su pago completo; las demás no.
        for (Reserva r : reservas) {
            boolean debeTenerPago = r.getEstado() == EstadoReserva.COMPLETADA || r.getEstado() == EstadoReserva.CONFIRMADA
                    || ReglasReserva.NO_ASISTIO.equals(r.getObservacion());
            assertThat(r.getPago() != null).as("pago de #%d (%s)", r.getId(), r.getEstado()).isEqualTo(debeTenerPago);
            if (debeTenerPago) {
                assertThat(r.getPago().getMonto()).isEqualByComparingTo(r.getServicio().getPrecio());
                assertThat(r.getPago().getFechaPago()).isBeforeOrEqualTo(AHORA);
            }
        }
        assertThat(pagoRepository.count()).isEqualTo(reservas.stream().filter(r -> r.getPago() != null).count());
    }

    @Test
    @DisplayName("Días laborables más cargados que el domingo y horas pico al abrir y por la tarde-noche")
    void distribucionRealista() {
        List<Reserva> reservas = reservaRepository.findAll();
        Map<LocalDate, Long> porDia = reservas.stream()
                .collect(Collectors.groupingBy(r -> r.getFechaHora().toLocalDate(), Collectors.counting()));

        double domingo = promedio(porDia, d -> d.getDayOfWeek() == DayOfWeek.SUNDAY);
        double laborable = promedio(porDia, d -> d.getDayOfWeek() != DayOfWeek.SUNDAY);
        assertThat(domingo).isLessThan(3.5);
        assertThat(laborable).isGreaterThan(domingo * 4);

        Map<LocalTime, Long> porHora = reservas.stream()
                .collect(Collectors.groupingBy(r -> r.getFechaHora().toLocalTime(), Collectors.counting()));
        long pico = porHora.getOrDefault(LocalTime.of(18, 0), 0L) + porHora.getOrDefault(LocalTime.of(18, 30), 0L)
                + porHora.getOrDefault(LocalTime.of(13, 0), 0L);
        long valle = porHora.getOrDefault(LocalTime.of(15, 0), 0L) + porHora.getOrDefault(LocalTime.of(15, 30), 0L)
                + porHora.getOrDefault(LocalTime.of(21, 30), 0L);
        assertThat(pico).isGreaterThan(valle * 3 / 2);
    }

    @Test
    @DisplayName("El futuro tiene días muy llenos, medio llenos y con pocas citas, pero ningún día laborable vacío")
    void futuroConHuecosPeroSinAgendaVacia() {
        Map<LocalDate, Long> porDia = reservaRepository.findAll().stream()
                .filter(r -> r.getFechaHora().toLocalDate().isAfter(HOY))
                .collect(Collectors.groupingBy(r -> r.getFechaHora().toLocalDate(), Collectors.counting()));
        List<Long> laborables = HOY.plusDays(1).datesUntil(LocalDate.of(2027, 1, 1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .map(d -> porDia.getOrDefault(d, 0L)).toList();

        assertThat(laborables).allSatisfy(n -> assertThat(n).isGreaterThanOrEqualTo(2));
        assertThat(laborables).anySatisfy(n -> assertThat(n).isGreaterThanOrEqualTo(18)); // muy llenos
        assertThat(laborables).anySatisfy(n -> assertThat(n).isBetween(9L, 15L));        // medio llenos
        assertThat(laborables).anySatisfy(n -> assertThat(n).isLessThanOrEqualTo(7));     // pocas citas
        assertThat(laborables).allSatisfy(n -> assertThat(n).isLessThan(GeneradorAgendaDemo.CAPACIDAD_DIA));
    }

    @Test
    @DisplayName("Sin solapes: ninguna terapeuta, cabina ni paciente tiene dos citas activas que se crucen")
    void noHayCitasSolapadas() {
        List<Reserva> activas = reservaRepository.findAll().stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA).toList();

        assertSinSolapes(activas, r -> r.getEmpleado().getId(), ReglasReserva::finBloque, "terapeuta");
        assertSinSolapes(activas, r -> r.getSala().getId(), ReglasReserva::finBloque, "cabina");
        assertSinSolapes(activas, r -> r.getCliente().getId(), ReglasReserva::finSesion, "paciente");
    }

    @Test
    @DisplayName("Datos 100% ficticios: correos @example.com y teléfonos de ficción; ningún DNI")
    void datosFicticios() {
        List<Cliente> clientes = clienteRepository.findAll();
        assertThat(clientes).hasSizeGreaterThanOrEqualTo(DemoDataSeeder.PACIENTES);
        assertThat(clientes).allSatisfy(c -> assertThat(c.getCorreo()).endsWith("@example.com"));
        assertThat(clientes).filteredOn(c -> !c.getCorreo().contains(".demo@"))
                .allSatisfy(c -> assertThat(c.getTelefono()).startsWith("900000"));
        assertThat(usuarioRepository.findAll()).allSatisfy(u -> assertThat(u.getCorreo()).endsWith("@example.com"));
    }

    @Test
    @DisplayName("Re-ejecutar el seeder no duplica pacientes, citas, pagos ni cuentas")
    void reEjecutarNoDuplica() throws Exception {
        long reservas = reservaRepository.count();
        long clientes = clienteRepository.count();
        long pagos = pagoRepository.count();
        long usuarios = usuarioRepository.count();

        seeder.run();

        assertThat(reservaRepository.count()).isEqualTo(reservas);
        assertThat(clienteRepository.count()).isEqualTo(clientes);
        assertThat(pagoRepository.count()).isEqualTo(pagos);
        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
    }

    @Test
    @DisplayName("Las 11 cuentas demo existen con su rol y nombre, usan DEMO_PASSWORD (guardada cifrada) y están vinculadas a su ficha")
    void cuentasDemoDocumentadas() {
        assertThat(DemoDataSeeder.CUENTAS).hasSize(11);
        for (DemoDataSeeder.CuentaDemo c : DemoDataSeeder.CUENTAS) {
            var u = usuarioRepository.findByCorreo(c.correo()).orElseThrow();
            assertThat(u.getRol()).isEqualTo(c.rol());
            assertThat(u.getNombre()).isEqualTo(c.nombre());
            assertThat(u.getPassword()).startsWith("$2").isNotEqualTo(CLAVE_DEMO);
            assertThat(passwordEncoder.matches(CLAVE_DEMO, u.getPassword())).as(c.usuario()).isTrue();
            assertThat(u.isActivo()).isTrue();
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
        var carlos = clienteRepository.findByCorreo("cliente.demo@example.com").orElseThrow();
        assertThat(fichaRepository.findByClienteId(carlos.getId())).isPresent();
    }

    private static double promedio(Map<LocalDate, Long> porDia, java.util.function.Predicate<LocalDate> filtro) {
        List<LocalDate> dias = HOY.minusDays(90).datesUntil(LocalDate.of(2027, 1, 1)).filter(filtro).toList();
        return dias.stream().mapToLong(d -> porDia.getOrDefault(d, 0L)).average().orElse(0);
    }

    /** Ordena por inicio dentro de cada recurso y comprueba que cada cita empiece después de que termine la anterior. */
    static void assertSinSolapes(List<Reserva> reservas, Function<Reserva, Long> recurso,
                                 Function<Reserva, LocalDateTime> fin, String tipo) {
        Map<Long, List<Reserva>> porRecurso = reservas.stream().collect(Collectors.groupingBy(recurso));
        porRecurso.forEach((id, lista) -> {
            List<Reserva> ordenadas = lista.stream().sorted(Comparator.comparing(Reserva::getFechaHora)).toList();
            for (int i = 1; i < ordenadas.size(); i++) {
                Reserva anterior = ordenadas.get(i - 1);
                Reserva actual = ordenadas.get(i);
                assertThat(actual.getFechaHora())
                        .as("%s %d: #%d (%s) se cruza con #%d (%s)", tipo, id, actual.getId(), actual.getFechaHora(),
                                anterior.getId(), anterior.getFechaHora())
                        .isAfterOrEqualTo(fin.apply(anterior));
            }
        });
    }
}
