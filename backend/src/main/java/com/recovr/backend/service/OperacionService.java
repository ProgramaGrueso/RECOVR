package com.recovr.backend.service;

import com.recovr.backend.dto.EmpleadoEstadoResponse;
import com.recovr.backend.dto.ResumenResponse;
import com.recovr.backend.dto.SalaEstadoResponse;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Estado en vivo del negocio. Nada se guarda: el estado de una cabina o de una terapeuta se DEDUCE de las
 * reservas y de la hora actual, así nunca queda desactualizado respecto de la agenda.
 *
 * <p>Cabina (para la hora {@code ahora}), en este orden de prioridad:
 * <ol>
 *   <li>OCUPADA: hay una reserva activa con check-in y {@code ahora} está dentro de la sesión.</li>
 *   <li>ESPERANDO_CLIENTE: hay una reserva activa cuya hora de inicio ya llegó pero el cliente aún no hace check-in.</li>
 *   <li>EN_SANITIZACION: una reserva COMPLETADA (o con sesión terminada) sigue dentro de su bloque, es decir
 *       antes de {@code fin de sesión + tiempo de limpieza}.</li>
 *   <li>DISPONIBLE: ninguna de las anteriores.</li>
 * </ol>
 * Terapeuta: EN_SESION (check-in y sesión en curso), POR_INICIAR (su sesión ya debió empezar y falta el check-in)
 * o LIBRE.
 */
@Service
public class OperacionService {

    private final SalaRepository salaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;

    public OperacionService(SalaRepository salaRepository, EmpleadoRepository empleadoRepository,
                            ReservaRepository reservaRepository, PagoRepository pagoRepository) {
        this.salaRepository = salaRepository;
        this.empleadoRepository = empleadoRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
    }

    @Transactional(readOnly = true)
    public List<SalaEstadoResponse> estadoSalas(LocalDateTime ahora) {
        List<Reserva> ventana = reservaRepository.buscarEntre(ahora.minusDays(1), ahora.plusDays(30));
        return salaRepository.findAll().stream()
                .sorted(Comparator.comparing(Sala::getNombre))
                .map(sala -> estadoDeSala(sala, ventana.stream()
                        .filter(r -> r.getSala().getId().equals(sala.getId())).toList(), ahora))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmpleadoEstadoResponse> estadoEmpleados(LocalDateTime ahora) {
        List<Reserva> ventana = reservaRepository.buscarEntre(ahora.minusDays(1), ahora.plusDays(30));
        return empleadoRepository.findAll().stream()
                .sorted(Comparator.comparing(Empleado::getNombre))
                .map(e -> estadoDeEmpleado(e, ventana.stream()
                        .filter(r -> r.getEmpleado().getId().equals(e.getId())).toList(), ahora))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime iniMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = iniMes.plusMonths(1);
        List<Reserva> delMes = reservaRepository.buscarEntre(iniMes, finMes);

        long completadas = delMes.stream().filter(r -> r.getEstado() == EstadoReserva.COMPLETADA).count();
        long canceladas = delMes.stream().filter(r -> r.getEstado() == EstadoReserva.CANCELADA).count();
        List<Reserva> deHoy = delMes.stream().filter(r -> r.getFechaHora().toLocalDate().equals(hoy)).toList();
        long agendadasHoy = deHoy.stream().filter(ReglasReserva::activa).count();
        long pendientes = delMes.stream().filter(r -> r.getEstado() == EstadoReserva.PENDIENTE
                && !r.getFechaHora().isBefore(ahora.minusMinutes(ReglasReserva.TOLERANCIA_CONFIRMACION_MIN))).count();

        long minutosAtencion = Duration.between(ReglasReserva.APERTURA, ReglasReserva.CIERRE).toMinutes();
        long cabinas = salaRepository.count();
        long minutosReservados = deHoy.stream().filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .mapToLong(ReglasReserva::duracion).sum();
        double ocupacion = cabinas == 0 ? 0 : redondear(100.0 * minutosReservados / (cabinas * minutosAtencion));
        double tasaCancelacion = delMes.isEmpty() ? 0 : redondear(100.0 * canceladas / delMes.size());
        BigDecimal ingresos = pagoRepository.sumarIngresos(iniMes, finMes);

        return new ResumenResponse(ingresos, Math.min(ocupacion, 100.0), completadas, agendadasHoy, pendientes,
                canceladas, delMes.size(), tasaCancelacion);
    }

    private SalaEstadoResponse estadoDeSala(Sala sala, List<Reserva> deLaSala, LocalDateTime ahora) {
        Optional<Reserva> ocupada = deLaSala.stream().filter(r -> ReglasReserva.activa(r) && r.getCheckIn() != null
                && !ahora.isBefore(r.getFechaHora()) && ahora.isBefore(ReglasReserva.finSesion(r))).findFirst();
        Optional<Reserva> esperando = deLaSala.stream().filter(r -> ReglasReserva.activa(r) && r.getCheckIn() == null
                && !ahora.isBefore(r.getFechaHora()) && ahora.isBefore(ReglasReserva.finSesion(r))).findFirst();
        Optional<Reserva> limpieza = deLaSala.stream().filter(r -> enLimpieza(r, ahora)).findFirst();
        LocalDateTime proxima = deLaSala.stream().filter(r -> ReglasReserva.activa(r) && r.getFechaHora().isAfter(ahora))
                .map(Reserva::getFechaHora).findFirst().orElse(null);

        if (ocupada.isPresent()) {
            Reserva r = ocupada.get();
            return new SalaEstadoResponse(sala.getId(), sala.getNombre(), sala.getCapacidad(), "OCUPADA",
                    r.getEmpleado().getNombre(), ReglasReserva.finSesion(r), proxima);
        }
        if (esperando.isPresent()) {
            Reserva r = esperando.get();
            return new SalaEstadoResponse(sala.getId(), sala.getNombre(), sala.getCapacidad(), "ESPERANDO_CLIENTE",
                    r.getEmpleado().getNombre(), r.getFechaHora().plusMinutes(ReglasReserva.TOLERANCIA_NO_SHOW_MIN), proxima);
        }
        if (limpieza.isPresent()) {
            Reserva r = limpieza.get();
            return new SalaEstadoResponse(sala.getId(), sala.getNombre(), sala.getCapacidad(), "EN_SANITIZACION",
                    r.getEmpleado().getNombre(), ReglasReserva.finBloque(r), proxima);
        }
        return new SalaEstadoResponse(sala.getId(), sala.getNombre(), sala.getCapacidad(), "DISPONIBLE", null, null, proxima);
    }

    private EmpleadoEstadoResponse estadoDeEmpleado(Empleado e, List<Reserva> deLa, LocalDateTime ahora) {
        Optional<Reserva> enSesion = deLa.stream().filter(r -> ReglasReserva.activa(r) && r.getCheckIn() != null
                && !ahora.isBefore(r.getFechaHora()) && ahora.isBefore(ReglasReserva.finSesion(r))).findFirst();
        Optional<Reserva> porIniciar = deLa.stream().filter(r -> ReglasReserva.activa(r) && r.getCheckIn() == null
                && !ahora.isBefore(r.getFechaHora()) && ahora.isBefore(ReglasReserva.finSesion(r))).findFirst();
        LocalDateTime proxima = deLa.stream().filter(r -> ReglasReserva.activa(r) && r.getFechaHora().isAfter(ahora))
                .map(Reserva::getFechaHora).findFirst().orElse(null);
        if (enSesion.isPresent()) {
            Reserva r = enSesion.get();
            return new EmpleadoEstadoResponse(e.getId(), e.getNombre(), e.getEspecialidad(), e.getTelefono(), e.isActivo(),
                    "EN_SESION", r.getSala().getNombre(), ReglasReserva.finSesion(r), proxima);
        }
        if (porIniciar.isPresent()) {
            Reserva r = porIniciar.get();
            return new EmpleadoEstadoResponse(e.getId(), e.getNombre(), e.getEspecialidad(), e.getTelefono(), e.isActivo(),
                    "POR_INICIAR", r.getSala().getNombre(), ReglasReserva.finSesion(r), proxima);
        }
        return new EmpleadoEstadoResponse(e.getId(), e.getNombre(), e.getEspecialidad(), e.getTelefono(), e.isActivo(),
                "LIBRE", null, null, proxima);
    }

    /** La cabina sigue en limpieza hasta el final del bloque de una reserva ya atendida. */
    private boolean enLimpieza(Reserva r, LocalDateTime ahora) {
        boolean atendida = r.getEstado() == EstadoReserva.COMPLETADA
                || (r.getEstado() == EstadoReserva.CONFIRMADA && r.getCheckIn() != null && !ahora.isBefore(ReglasReserva.finSesion(r)));
        return atendida && !ahora.isBefore(r.getFechaHora()) && ahora.isBefore(ReglasReserva.finBloque(r));
    }

    private static double redondear(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
