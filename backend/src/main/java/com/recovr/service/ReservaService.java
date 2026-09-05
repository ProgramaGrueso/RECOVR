package com.recovr.service;

import com.recovr.exception.ReservaConflictException;
import com.recovr.exception.ReservaNotFoundException;
import com.recovr.model.EstadoReserva;
import com.recovr.model.Reserva;
import com.recovr.model.Servicio;
import com.recovr.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio central del Core Engine para gestionar la lógica de reservas.
 * Demuestra Dependency Injection mediante Constructor Injection.
 */
@Service
public class ReservaService {

    private static final int DURACION_ESTANDAR_MINUTOS = 60;

    private final ReservaRepository reservaRepository;

    /**
     * Inyección de Dependencias por Constructor:
     * - Inyectamos la interfaz ReservaRepository (puerto desacoplado).
     * - Spring resuelve y provee la implementación activa (@Repository).
     * - Facilita pruebas unitarias sin levantar contexto pesado ni base de datos real.
     */
    public ReservaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /**
     * Calcula la finalización de un bloque sumando duración del servicio y tiempo de limpieza.
     */
    public LocalDateTime calcularFinBloque(LocalDateTime inicio, Servicio servicio) {
        int minutosTotales = servicio.getDuracionMinutos();
        if (servicio.getTiempoLimpiezaMinutos() != null) {
            minutosTotales += servicio.getTiempoLimpiezaMinutos();
        }
        return inicio.plusMinutes(minutosTotales);
    }

    /**
     * Valida que no existan reservas activas (PENDIENTE o CONFIRMADA)
     * que se solapen con el horario solicitado para el especialista o para la suite de tratamiento.
     */
    public void validarDisponibilidad(Long empleadoId, Long salaId, LocalDateTime inicio, int duracionMinutos) {
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);

        if (existeConflicto(reservaRepository.findByEmpleadoId(empleadoId), inicio, fin)) {
            throw new ReservaConflictException("El especialista seleccionado no se encuentra disponible en el turno solicitado.");
        }

        if (existeConflicto(reservaRepository.findBySalaId(salaId), inicio, fin)) {
            throw new ReservaConflictException("La suite o sala de tratamiento seleccionada ya se encuentra ocupada.");
        }
    }

    /**
     * Registra una nueva reserva validando disponibilidad y asignando el estado PENDIENTE.
     */
    public Reserva crearReserva(Reserva reserva) {
        validarDatosReserva(reserva);

        if (reserva.getDuracionTotalMinutos() == null) {
            reserva.setDuracionTotalMinutos(DURACION_ESTANDAR_MINUTOS);
        }

        validarDisponibilidad(
                reserva.getEmpleadoId(),
                reserva.getSalaId(),
                reserva.getFechaHora(),
                reserva.getDuracionTotalMinutos()
        );

        reserva.setEstado(EstadoReserva.PENDIENTE);
        return reservaRepository.save(reserva);
    }

    /**
     * Transición de estado: PENDIENTE -> CONFIRMADA.
     */
    public Reserva confirmarReserva(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new IllegalStateException("No se puede confirmar una reserva que ha sido previamente cancelada.");
        }
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepository.save(reserva);
    }

    /**
     * Transición de estado: Cancelar reserva.
     * No se permite cancelar sesiones ya COMPLETADAS.
     */
    public Reserva cancelarReserva(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva.getEstado() == EstadoReserva.COMPLETADA) {
            throw new IllegalStateException("No se puede cancelar una sesión que ya ha sido completada.");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    /**
     * Búsqueda por ID con manejo de excepción de recurso inexistente.
     */
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaNotFoundException("No existe reserva con identificador: " + id));
    }

    private void validarDatosReserva(Reserva reserva) {
        if (reserva.getFechaHora() == null) {
            throw new IllegalArgumentException("La fecha y hora de la reserva es requerida.");
        }
        if (reserva.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden registrar reservas en fechas u horas pasadas.");
        }
        if (reserva.getClienteId() == null || reserva.getEmpleadoId() == null ||
                reserva.getServicioId() == null || reserva.getSalaId() == null) {
            throw new IllegalArgumentException("Los identificadores de cliente, especialista, servicio y sala son obligatorios.");
        }
        if (reserva.getDuracionTotalMinutos() != null && reserva.getDuracionTotalMinutos() <= 0) {
            throw new IllegalArgumentException("La duración de la reserva debe ser un valor positivo.");
        }
        if (reserva.getMontoTotal() != null && reserva.getMontoTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto total de la reserva no puede ser negativo.");
        }
    }

    private boolean existeConflicto(List<Reserva> reservas, LocalDateTime inicio, LocalDateTime fin) {
        return reservas.stream()
                .anyMatch(r -> estaActiva(r) && haySolapamiento(r.getFechaHora(), r.getDuracionTotalMinutos(), inicio, fin));
    }

    private boolean estaActiva(Reserva r) {
        return r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.CONFIRMADA;
    }

    private boolean haySolapamiento(LocalDateTime inicio1, Integer duracion1, LocalDateTime inicio2, LocalDateTime fin2) {
        int dur = (duracion1 != null && duracion1 > 0) ? duracion1 : DURACION_ESTANDAR_MINUTOS;
        LocalDateTime fin1 = inicio1.plusMinutes(dur);
        return inicio1.isBefore(fin2) && inicio2.isBefore(fin1);
    }
}
