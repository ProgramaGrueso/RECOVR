package com.recovr.backend.config;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.service.ReglasReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias (sin Spring ni base de datos) del generador de la agenda demo. */
class GeneradorAgendaDemoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 15, 0);
    private final AtomicLong ids = new AtomicLong();

    @Test
    @DisplayName("Es determinista: el mismo día genera exactamente las mismas citas")
    void determinista() {
        GeneradorAgendaDemo.Recursos recursos = recursos();
        List<Reserva> a = new GeneradorAgendaDemo(recursos, AHORA).generarDia(LocalDate.of(2026, 11, 20));
        List<Reserva> b = new GeneradorAgendaDemo(recursos, AHORA).generarDia(LocalDate.of(2026, 11, 20));

        assertThat(a).isNotEmpty();
        assertThat(a).extracting(r -> r.getFechaHora() + r.getEmpleado().getNombre() + r.getCliente().getCorreo() + r.getEstado())
                .containsExactlyElementsOf(b.stream()
                        .map(r -> r.getFechaHora() + r.getEmpleado().getNombre() + r.getCliente().getCorreo() + r.getEstado()).toList());
    }

    @Test
    @DisplayName("Los días futuros se reparten entre llenos, medios y tranquilos (no todos iguales)")
    void tiposDeDiaVariados() {
        Map<GeneradorAgendaDemo.TipoDia, Long> tipos = new EnumMap<>(GeneradorAgendaDemo.TipoDia.class);
        LocalDate.of(2026, 11, 5).datesUntil(LocalDate.of(2027, 1, 1))
                .forEach(d -> tipos.merge(GeneradorAgendaDemo.tipoDia(d), 1L, Long::sum));

        assertThat(tipos).containsOnlyKeys(GeneradorAgendaDemo.TipoDia.values());
        assertThat(tipos.values()).allSatisfy(n -> assertThat(n).isGreaterThanOrEqualTo(8));
    }

    @Test
    @DisplayName("Sin solapes de terapeuta, cabina ni paciente en todo el rango generado")
    void sinSolapes() {
        List<Reserva> todas = new GeneradorAgendaDemo(recursos(), AHORA)
                .generar(AHORA.toLocalDate().minusDays(90), LocalDate.of(2026, 12, 31));
        todas.forEach(r -> r.setId(ids.incrementAndGet()));
        List<Reserva> activas = todas.stream().filter(r -> r.getEstado() != EstadoReserva.CANCELADA).toList();

        DemoDataSeederTest.assertSinSolapes(activas, r -> r.getEmpleado().getId(), ReglasReserva::finBloque, "terapeuta");
        DemoDataSeederTest.assertSinSolapes(activas, r -> r.getSala().getId(), ReglasReserva::finBloque, "cabina");
        DemoDataSeederTest.assertSinSolapes(activas, r -> r.getCliente().getId(), ReglasReserva::finSesion, "paciente");
        // Incluso las canceladas se generaron sin cruces (el hueco que dejan es el de una cancelación real)
        DemoDataSeederTest.assertSinSolapes(todas, r -> r.getEmpleado().getId(), ReglasReserva::finBloque, "terapeuta (todas)");
    }

    @Test
    @DisplayName("Pacientes ficticios estables: mismo nombre y correo @example.com en cada ejecución, sin repetir correo")
    void pacientesEstables() {
        List<PacientesDemo.Paciente> a = PacientesDemo.generar(140);
        assertThat(a).isEqualTo(PacientesDemo.generar(140));
        assertThat(a).extracting(PacientesDemo.Paciente::correo).doesNotHaveDuplicates()
                .allSatisfy(c -> assertThat(c).matches("[a-z]+\\.[a-z]+\\.\\d{3}@example\\.com"));
        assertThat(a).extracting(PacientesDemo.Paciente::nombre).allSatisfy(n -> assertThat(n.split(" ")).hasSize(3));
        assertThat(PacientesDemo.slug("Ñahui Álvaro")).isEqualTo("nahuialvaro");
    }

    private GeneradorAgendaDemo.Recursos recursos() {
        Map<String, Servicio> servicios = new HashMap<>();
        servicios.put("RC-01", servicio(60, 15, "200"));
        servicios.put("RC-02", servicio(75, 15, "280"));
        servicios.put("RC-03", servicio(60, 15, "180"));
        servicios.put("RC-04", servicio(75, 15, "250"));
        servicios.put("RC-05", servicio(60, 15, "380"));
        servicios.put("RC-06", servicio(90, 20, "450"));
        Map<String, Sala> cabinas = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            Sala s = new Sala();
            s.setId(ids.incrementAndGet());
            s.setNombre("Cabina 0" + i + " ");
            cabinas.put("Cabina 0" + i + " ", s);
        }
        Map<String, Empleado> terapeutas = new HashMap<>();
        for (String nombre : List.of("Ely", "Miranda", "Pamela", "Maria")) {
            Empleado e = new Empleado();
            e.setId(ids.incrementAndGet());
            e.setNombre(nombre);
            terapeutas.put(nombre, e);
        }
        List<Cliente> pacientes = PacientesDemo.generar(140).stream().map(p -> {
            Cliente c = new Cliente();
            c.setId(ids.incrementAndGet());
            c.setNombre(p.nombre());
            c.setCorreo(p.correo());
            return c;
        }).collect(Collectors.toList());
        return new GeneradorAgendaDemo.Recursos(servicios, cabinas, terapeutas, pacientes);
    }

    private Servicio servicio(int duracion, int limpieza, String precio) {
        Servicio s = new Servicio();
        s.setId(ids.incrementAndGet());
        s.setDuracionMinutos(duracion);
        s.setTiempoLimpiezaMinutos(limpieza);
        s.setPrecio(new BigDecimal(precio));
        return s;
    }
}
