package com.recovr.backend.config;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.service.ReglasReserva;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Genera, en memoria y sin tocar la base, la agenda de demostración de un día: qué citas hubo (o habrá), con qué
 * terapeuta, en qué cabina, para qué paciente y en qué estado.
 *
 * <p>Reglas que respeta (las mismas que el motor de reservas):
 * <ul>
 *   <li>Ninguna terapeuta ni cabina tiene dos citas cuyo bloque [inicio, fin de sesión + limpieza) se cruce.</li>
 *   <li>Un paciente no está en dos sesiones a la vez.</li>
 *   <li>Toda la sesión cabe en el horario de atención ({@link ReglasReserva#APERTURA}–{@link ReglasReserva#CIERRE}).</li>
 * </ul>
 *
 * <p>Distribución: de lunes a sábado hay más carga (viernes y sábado los más llenos) y el domingo casi no hay citas.
 * Las horas pico son las primeras de la jornada (el negocio abre a mediodía, así que la "media mañana" de un
 * consultorio tradicional aquí cae entre 12:30 y 14:00) y la tarde-noche (17:30–20:00).
 * El pasado se ve como una agenda trabajada; en el futuro cada día es {@link TipoDia#LLENO}, {@link TipoDia#MEDIO} o
 * {@link TipoDia#TRANQUILO} para que quede espacio para agendar sin que la agenda se vea vacía.
 *
 * <p>Es determinista: el mismo día con el mismo catálogo y el mismo "ahora" produce siempre las mismas citas.
 */
final class GeneradorAgendaDemo {

    static final long SEMILLA = 20_260_806L;
    /** Citas que caben como máximo en un día con 4 terapeutas (unas 6–7 por terapeuta). */
    static final int CAPACIDAD_DIA = 26;

    enum TipoDia { LLENO, MEDIO, TRANQUILO }

    /** Horarios de inicio posibles (cada 30 min) y su peso relativo: dos picos, al abrir y por la tarde-noche. */
    private static final LocalTime[] HORAS = new LocalTime[20];
    private static final int[] PESO_HORA = {4, 6, 7, 5, 3, 2, 1, 1, 2, 3, 4, 6, 8, 8, 7, 5, 4, 3, 2, 1};

    static {
        for (int i = 0; i < HORAS.length; i++) {
            HORAS[i] = LocalTime.of(12, 0).plusMinutes(30L * i); // 12:00 … 21:30
        }
    }

    private static final String[] CODIGOS = {"RC-01", "RC-02", "RC-03", "RC-04", "RC-05", "RC-06"};
    private static final int[] PESO_SERVICIO = {25, 18, 22, 15, 8, 12};
    private static final String[] METODOS_PAGO = {"Yape", "Yape", "Yape", "Plin", "Plin", "Tarjeta", "Tarjeta", "Tarjeta", "Efectivo"};

    /** Recursos del catálogo con los que se arma la agenda (servicios por código, cabinas por prefijo, terapeutas por nombre). */
    record Recursos(Map<String, Servicio> servicios, Map<String, Sala> cabinas,
                    Map<String, Empleado> terapeutas, List<Cliente> pacientes) {}

    private final Recursos recursos;
    private final LocalDateTime ahora;

    GeneradorAgendaDemo(Recursos recursos, LocalDateTime ahora) {
        this.recursos = recursos;
        this.ahora = ahora;
    }

    /** Terapeutas que pueden atender cada servicio (la primera es la titular según la carta oficial). */
    static List<String> terapeutasDe(String codigo) {
        return switch (codigo) {
            case "RC-01" -> List.of("Ely", "Maria");
            case "RC-02" -> List.of("Miranda", "Maria");
            case "RC-03" -> List.of("Pamela", "Ely");
            case "RC-04" -> List.of("Ely", "Miranda");
            case "RC-05" -> List.of("Pamela");
            default -> List.of("Maria");
        };
    }

    /** Cabinas aptas para cada servicio (por prefijo "Cabina 0X "). */
    static List<String> cabinasDe(String codigo) {
        return switch (codigo) {
            case "RC-02" -> List.of("Cabina 02 ");
            case "RC-05" -> List.of("Cabina 03 ");
            case "RC-06" -> List.of("Cabina 04 ");
            default -> List.of("Cabina 01 ", "Cabina 03 ");
        };
    }

    /** Tipo de un día futuro: fijo para cada fecha, así la agenda no cambia entre ejecuciones. */
    static TipoDia tipoDia(LocalDate dia) {
        double x = aleatorio(SEMILLA * 31 + dia.toEpochDay()).nextDouble();
        return x < 0.25 ? TipoDia.LLENO : x < 0.65 ? TipoDia.MEDIO : TipoDia.TRANQUILO;
    }

    /** Fracción de la capacidad del día que se intenta ocupar. */
    double carga(LocalDate dia, Random rnd) {
        DayOfWeek dow = dia.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY) {
            return 0.03 + rnd.nextDouble() * 0.06; // domingo casi vacío: 0 a 3 citas
        }
        if (!dia.isAfter(ahora.toLocalDate())) {
            double base = switch (dow) {
                case MONDAY -> 0.62;
                case TUESDAY -> 0.66;
                case WEDNESDAY -> 0.70;
                case THURSDAY -> 0.74;
                case FRIDAY -> 0.84;
                default -> 0.80; // sábado
            };
            return base * (0.85 + rnd.nextDouble() * 0.25);
        }
        double ajuste = switch (dow) {
            case MONDAY -> 0.9;
            case FRIDAY, SATURDAY -> 1.08;
            default -> 1.0;
        };
        return ajuste * switch (tipoDia(dia)) {
            case LLENO -> 0.88;
            case MEDIO -> 0.52;
            case TRANQUILO -> 0.22;
        };
    }

    /** Agenda de todos los días del rango, ambos inclusive. */
    List<Reserva> generar(LocalDate desde, LocalDate hasta) {
        List<Reserva> todas = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            todas.addAll(generarDia(dia));
        }
        return todas;
    }

    /** Citas de un día, ya con estado, fechas de creación/pago/cancelación y pago cuando corresponde. */
    List<Reserva> generarDia(LocalDate dia) {
        Random rnd = aleatorio(SEMILLA + dia.toEpochDay());
        int objetivo = (int) Math.floor(CAPACIDAD_DIA * carga(dia, rnd) + rnd.nextDouble());
        Map<Object, List<LocalDateTime[]>> ocupado = new HashMap<>();
        List<Reserva> delDia = new ArrayList<>();
        // Pocos reintentos a propósito: si un horario está tomado no se fuerza otro, así se conservan las horas pico.
        for (int intento = 0; intento < objetivo * 3 && delDia.size() < objetivo; intento++) {
            Reserva r = intentarCita(dia, rnd, ocupado);
            if (r != null) {
                delDia.add(r);
            }
        }
        delDia.sort((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()));
        for (Reserva r : delDia) {
            asignarEstado(r, rnd);
        }
        return delDia;
    }

    private Reserva intentarCita(LocalDate dia, Random rnd, Map<Object, List<LocalDateTime[]>> ocupado) {
        String codigo = CODIGOS[elegirPonderado(PESO_SERVICIO, rnd)];
        Servicio servicio = recursos.servicios().get(codigo);
        LocalTime hora = HORAS[elegirPonderado(PESO_HORA, rnd)];
        Cliente paciente = elegirPaciente(rnd);
        if (servicio == null || paciente == null) {
            return null;
        }
        LocalDateTime inicio = dia.atTime(hora);
        LocalDateTime finSesion = inicio.plusMinutes(duracion(servicio));
        LocalDateTime finBloque = finSesion.plusMinutes(limpieza(servicio));
        if (!ReglasReserva.dentroDeHorario(inicio, finSesion) || ocupado(ocupado, paciente, inicio, finSesion)) {
            return null;
        }
        Empleado terapeuta = primeraLibre(terapeutasDe(codigo).stream().map(recursos.terapeutas()::get).toList(),
                ocupado, inicio, finBloque);
        Sala cabina = primeraLibre(cabinasDe(codigo).stream().map(recursos.cabinas()::get).toList(),
                ocupado, inicio, finBloque);
        if (terapeuta == null || cabina == null) {
            return null;
        }
        ocupar(ocupado, terapeuta, inicio, finBloque);
        ocupar(ocupado, cabina, inicio, finBloque);
        ocupar(ocupado, paciente, inicio, finSesion);
        Reserva r = new Reserva();
        r.setCliente(paciente);
        r.setEmpleado(terapeuta);
        r.setSala(cabina);
        r.setServicio(servicio);
        r.setFechaHora(inicio);
        return r;
    }

    /**
     * Estados realistas. Pasado: la mayoría completadas, algunas canceladas (por el paciente o porque no se pagaron a
     * tiempo) y algunas "no asistió" (pagadas, sin check-in). Futuro: confirmadas (pagadas) y algunas pendientes.
     * Una sesión que empezó y aún no termina queda CONFIRMADA con check-in ("en sesión").
     */
    private void asignarEstado(Reserva r, Random rnd) {
        LocalDateTime inicio = r.getFechaHora();
        double x = rnd.nextDouble();
        LocalDateTime creada = inicio.minusMinutes(360 + rnd.nextInt(60 * 24 * 12));
        if (creada.isAfter(ahora)) {
            creada = ahora.minusMinutes(10 + rnd.nextInt(60 * 24 * 3));
        }
        r.setCreadaEn(creada);
        LocalDateTime pagada = minimo(creada.plusMinutes(1 + rnd.nextInt(ReglasReserva.HOLD_PAGO_MIN - 1)), ahora);

        if (!inicio.isAfter(ahora)) {
            LocalDateTime finSesion = inicio.plusMinutes(duracion(r.getServicio()));
            if (finSesion.isAfter(ahora)) {
                r.setEstado(EstadoReserva.CONFIRMADA);
                r.setCheckIn(inicio.minusMinutes(rnd.nextInt(10)));
                pagar(r, pagada, rnd);
            } else if (x < 0.80) {
                r.setEstado(EstadoReserva.COMPLETADA);
                r.setCheckIn(inicio.minusMinutes(rnd.nextInt(12)));
                pagar(r, pagada, rnd);
            } else if (x < 0.86) {
                cancelar(r, creada.plusMinutes(ReglasReserva.HOLD_PAGO_MIN), ReglasReserva.EXPIRADA_SIN_PAGO);
            } else if (x < 0.93) {
                cancelar(r, creada.plusMinutes(1 + rnd.nextInt(ReglasReserva.HOLD_PAGO_MIN - 1)), ReglasReserva.CANCELADA_POR_CLIENTE);
            } else {
                cancelar(r, inicio.plusMinutes(ReglasReserva.TOLERANCIA_NO_SHOW_MIN), ReglasReserva.NO_ASISTIO);
                pagar(r, pagada, rnd);
            }
            return;
        }
        boolean proxima = inicio.isBefore(ahora.plusDays(3));
        if (x < (proxima ? 0.93 : 0.82)) {
            r.setEstado(EstadoReserva.CONFIRMADA);
            pagar(r, pagada, rnd);
        } else {
            // Pendiente de pago sin fecha de creación: el planificador no la expira hasta la hora de la sesión.
            r.setEstado(EstadoReserva.PENDIENTE);
            r.setCreadaEn(null);
        }
    }

    private static void cancelar(Reserva r, LocalDateTime cuando, String motivo) {
        r.setEstado(EstadoReserva.CANCELADA);
        r.setCanceladaEn(cuando);
        r.setObservacion(motivo);
    }

    private static void pagar(Reserva r, LocalDateTime cuando, Random rnd) {
        Pago p = new Pago();
        p.setReserva(r);
        p.setMonto(r.getServicio().getPrecio());
        p.setMetodoPago(METODOS_PAGO[rnd.nextInt(METODOS_PAGO.length)]);
        p.setFechaPago(cuando);
        r.setPago(p);
    }

    /** Elige paciente con preferencia por los primeros de la lista: hay clientes frecuentes y otros ocasionales. */
    private Cliente elegirPaciente(Random rnd) {
        List<Cliente> pacientes = recursos.pacientes();
        if (pacientes.isEmpty()) {
            return null;
        }
        return pacientes.get((int) Math.floor(Math.pow(rnd.nextDouble(), 1.7) * pacientes.size()));
    }

    private static <T> T primeraLibre(List<T> candidatos, Map<Object, List<LocalDateTime[]>> ocupado,
                                      LocalDateTime desde, LocalDateTime hasta) {
        return candidatos.stream().filter(Objects::nonNull)
                .filter(c -> !ocupado(ocupado, c, desde, hasta)).findFirst().orElse(null);
    }

    private static boolean ocupado(Map<Object, List<LocalDateTime[]>> ocupado, Object recurso,
                                   LocalDateTime desde, LocalDateTime hasta) {
        return ocupado.getOrDefault(recurso, List.of()).stream()
                .anyMatch(b -> desde.isBefore(b[1]) && b[0].isBefore(hasta));
    }

    private static void ocupar(Map<Object, List<LocalDateTime[]>> ocupado, Object recurso,
                               LocalDateTime desde, LocalDateTime hasta) {
        ocupado.computeIfAbsent(recurso, k -> new ArrayList<>()).add(new LocalDateTime[]{desde, hasta});
    }

    /**
     * {@link Random} con la semilla mezclada (SplitMix64): semillas consecutivas (días seguidos) darían primeros
     * valores casi iguales y todos los días saldrían del mismo tipo.
     */
    static Random aleatorio(long semilla) {
        long z = semilla + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return new Random(z ^ (z >>> 31));
    }

    private static int elegirPonderado(int[] pesos, Random rnd) {
        int total = 0;
        for (int p : pesos) {
            total += p;
        }
        int x = rnd.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            x -= pesos[i];
            if (x < 0) {
                return i;
            }
        }
        return pesos.length - 1;
    }

    private static int duracion(Servicio s) {
        return s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 60;
    }

    private static int limpieza(Servicio s) {
        return s.getTiempoLimpiezaMinutos() != null ? s.getTiempoLimpiezaMinutos() : 0;
    }

    private static LocalDateTime minimo(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
