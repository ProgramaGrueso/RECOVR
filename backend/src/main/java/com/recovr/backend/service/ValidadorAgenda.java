package com.recovr.backend.service;

import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.exception.ReservaBloqueadaException;
import com.recovr.backend.exception.ReservaConflictException;
import com.recovr.backend.repository.AusenciaEmpleadoRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglas de agenda que comparten crear, reprogramar y reabrir una reserva: horario de atención, que la terapeuta,
 * la cabina y el propio cliente no tengan otra sesión que se cruce, ausencias, cabinas en mantenimiento y los límites
 * contra el acaparamiento de cupos. Se ejecuta dentro de la transacción de {@link ReservaService}, con los recursos
 * ya bloqueados, así que las consultas ven todo lo que otras reservas confirmaron.
 */
@Component
public class ValidadorAgenda {

    private final ReservaRepository reservaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServicioRepository servicioRepository;
    private final SalaRepository salaRepository;
    private final AusenciaEmpleadoRepository ausenciaRepository;

    public ValidadorAgenda(ReservaRepository reservaRepository, EmpleadoRepository empleadoRepository,
                           ServicioRepository servicioRepository, SalaRepository salaRepository,
                           AusenciaEmpleadoRepository ausenciaRepository) {
        this.reservaRepository = reservaRepository;
        this.empleadoRepository = empleadoRepository;
        this.servicioRepository = servicioRepository;
        this.salaRepository = salaRepository;
        this.ausenciaRepository = ausenciaRepository;
    }

    /** Datos mínimos para poder evaluar cualquier regla de agenda. */
    private void exigirDatosCompletos(Reserva reserva) {
        if (reserva.getSala() == null || reserva.getEmpleado() == null || reserva.getServicio() == null) {
            throw new IllegalArgumentException("La reserva debe indicar sala, especialista y servicio");
        }
    }

    /** La sesión completa debe caber en el horario de atención (el mismo que usa la ocupación diaria). */
    public void validarReglasDeAgenda(Reserva reserva) {
        exigirDatosCompletos(reserva);
        LocalDateTime inicio = reserva.getFechaHora();
        LocalDateTime finSesion = ReglasReserva.finSesion(reserva);
        if (!ReglasReserva.dentroDeHorario(inicio, finSesion)) {
            throw new IllegalArgumentException("La sesión (" + inicio.toLocalTime().withSecond(0).withNano(0) + " a "
                    + finSesion.toLocalTime().withSecond(0).withNano(0) + ") debe estar dentro del horario de atención: "
                    + ReglasReserva.APERTURA + " a " + ReglasReserva.CIERRE + ".");
        }
    }

    /**
     * Reglas que protegen la agenda de un uso abusivo: una persona no puede estar en dos sesiones a la vez, no puede
     * retener cupos sin pagar y quien deja expirar reservas repetidamente queda bloqueado un tiempo.
     */
    public void validarCliente(Reserva reserva, Long excluirId, boolean nueva, LocalDateTime ahora) {
        Long clienteId = reserva.getCliente().getId();
        LocalDateTime inicio = reserva.getFechaHora();
        List<Reserva> propias = reservaRepository.buscarConflictosCliente(clienteId, inicio,
                ReglasReserva.finSesion(reserva), excluirId);
        if (!propias.isEmpty()) {
            throw new ReservaConflictException("El cliente ya tiene otra reserva que se cruza con ese horario: "
                    + "una persona no puede estar en dos sesiones a la vez.");
        }
        if (nueva && reserva.getEstado() == EstadoReserva.PENDIENTE) {
            if (reservaRepository.countByClienteIdAndEstadoAndCreadaEnIsNotNull(clienteId, EstadoReserva.PENDIENTE) >= ReglasReserva.MAX_PENDIENTES_CLIENTE) {
                throw new ReservaConflictException("Ya tienes una reserva pendiente de pago. Págala (tienes "
                        + ReglasReserva.HOLD_PAGO_MIN + " min desde que la creaste) o cancélala antes de reservar otro turno.");
            }
            long expiradas = reservaRepository.countByClienteIdAndObservacionAndCanceladaEnGreaterThanEqual(clienteId,
                    ReglasReserva.EXPIRADA_SIN_PAGO, ahora.minusHours(ReglasReserva.VENTANA_EXPIRADAS_HORAS));
            if (expiradas >= ReglasReserva.MAX_EXPIRADAS_VENTANA) {
                throw new ReservaBloqueadaException("Dejaste vencer " + expiradas + " reservas sin pagar en las últimas "
                        + ReglasReserva.VENTANA_EXPIRADAS_HORAS + " horas. Por seguridad de la agenda, reserva de nuevo mañana o pide a recepción que lo haga en el mostrador.");
            }
        }
    }

    /**
     * Regla de negocio central (misma que el Core Engine del APF1): ningún especialista ni sala
     * puede tener dos reservas activas cuyos bloques [inicio, inicio + duración + limpieza) se crucen.
     * Se evalúa con los recursos ya bloqueados, así que la consulta ve todo lo que otras reservas confirmaron.
     */
    public void validarDisponibilidad(Reserva reserva, Long excluirId) {
        exigirDatosCompletos(reserva);
        LocalDateTime inicio = reserva.getFechaHora();
        LocalDateTime fin = ReglasReserva.finBloque(inicio, reserva.getServicio());
        if (!reserva.getSala().isActiva()) {
            throw new ReservaConflictException(reserva.getSala().getNombre() + " está en mantenimiento y no recibe reservas.");
        }
        if (!ausenciaRepository.buscarCruces(reserva.getEmpleado().getId(), inicio, fin).isEmpty()) {
            throw new ReservaConflictException(reserva.getEmpleado().getNombre() + " no atiende en ese horario (ausencia programada).");
        }
        List<Reserva> conflictos = reservaRepository.buscarConflictos(
                reserva.getSala().getId(), reserva.getEmpleado().getId(), inicio, fin, excluirId);

        boolean especialistaOcupado = conflictos.stream()
                .anyMatch(r -> r.getEmpleado().getId().equals(reserva.getEmpleado().getId()));
        if (especialistaOcupado) {
            throw new ReservaConflictException("El especialista seleccionado no se encuentra disponible en el turno solicitado.");
        }
        if (!conflictos.isEmpty()) {
            throw new ReservaConflictException("La suite o sala de tratamiento seleccionada ya se encuentra ocupada.");
        }
    }

    public void asignarRelaciones(Reserva reserva, ReservaRequest request) {
        reserva.setEmpleado(empleadoRepository.buscarBloqueado(request.empleadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", request.empleadoId())));
        if (!reserva.getEmpleado().isActivo()) {
            throw new IllegalArgumentException(reserva.getEmpleado().getNombre() + " está dada de baja y no recibe reservas");
        }
        reserva.setServicio(servicioRepository.findById(request.servicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio", request.servicioId())));
        reserva.setFechaHora(request.fechaHora());
        if (request.salaId() != null) {
            reserva.setSala(salaRepository.buscarBloqueada(request.salaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sala", request.salaId())));
        } else if (reserva.getSala() == null || !salaActualSigueLibre(reserva)) {
            // Sin cabina indicada: se conserva la actual si sigue libre en el nuevo horario; si no, la primera libre.
            reserva.setSala(asignarSalaLibre(reserva));
        }
    }

    /** Bloquea la cabina actual y comprueba que siga libre y en servicio en el horario (ya actualizado) de la reserva. */
    private boolean salaActualSigueLibre(Reserva reserva) {
        Sala actual = salaRepository.buscarBloqueada(reserva.getSala().getId()).orElse(null);
        if (actual == null || !actual.isActiva()) {
            return false;
        }
        LocalDateTime fin = ReglasReserva.finBloque(reserva.getFechaHora(), reserva.getServicio());
        return reservaRepository.buscarConflictos(actual.getId(), -1L, reserva.getFechaHora(), fin, reserva.getId()).isEmpty();
    }

    /**
     * Sin sala indicada se usa la primera cabina libre durante todo el bloque (duración + limpieza). Cada candidata
     * se bloquea y se vuelve a comprobar: si otra reserva la tomó mientras tanto, se pasa a la siguiente.
     */
    private Sala asignarSalaLibre(Reserva reserva) {
        if (reserva.getFechaHora() == null) {
            throw new IllegalArgumentException("Debe indicar la fecha y hora de la reserva");
        }
        LocalDateTime fin = ReglasReserva.finBloque(reserva.getFechaHora(), reserva.getServicio());
        for (Sala candidata : salaRepository.buscarDisponibles(reserva.getFechaHora(), fin)) {
            Sala bloqueada = salaRepository.buscarBloqueada(candidata.getId()).orElse(null);
            if (bloqueada != null && reservaRepository.buscarConflictos(bloqueada.getId(), -1L,
                    reserva.getFechaHora(), fin, reserva.getId()).isEmpty()) {
                return bloqueada;
            }
        }
        throw new ReservaConflictException("No hay cabinas disponibles en el turno solicitado.");
    }
}
