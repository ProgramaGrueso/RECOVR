package com.recovr.backend.repository;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Expresión JPQL reutilizable: una reserva ocupa el bloque
     * [fechaHora, fechaHora + duración del servicio + tiempo de limpieza).
     * Si el servicio no define duración se asume el estándar de 60 minutos (igual que el Core Engine).
     */
    String FIN_BLOQUE = "(r.fechaHora + (COALESCE(r.servicio.duracionMinutos, 60) + "
            + "COALESCE(r.servicio.tiempoLimpiezaMinutos, 0)) minute)";

    String ACTIVA = "r.estado IN (com.recovr.backend.entity.EstadoReserva.PENDIENTE, "
            + "com.recovr.backend.entity.EstadoReserva.CONFIRMADA)";

    boolean existsByClienteId(Long clienteId);
    /** Pendientes con plazo de pago en curso (las anteriores a existir la fecha de creación, p. ej. datos demo, no cuentan). */
    long countByClienteIdAndEstadoAndCreadaEnIsNotNull(Long clienteId, EstadoReserva estado);

    /** Lee la reserva bloqueando su fila: dos cobros o cancelaciones simultáneos de la misma reserva se hacen en fila. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id")
    Optional<Reserva> buscarParaActualizar(@Param("id") Long id);

    /** Reservas PENDIENTES cuyo plazo de pago (contado desde la creación) ya venció. */
    @Query("SELECT r FROM Reserva r WHERE r.estado = com.recovr.backend.entity.EstadoReserva.PENDIENTE "
            + "AND r.creadaEn IS NOT NULL AND r.creadaEn <= :limite")
    List<Reserva> buscarPendientesCreadasHasta(@Param("limite") LocalDateTime limite);

    /**
     * Reservas activas del mismo cliente cuya sesión (sin limpieza) se cruza con [inicio, fin): una persona no puede
     * estar en dos sesiones a la vez.
     */
    @Query("SELECT r FROM Reserva r WHERE " + ACTIVA + " AND r.cliente.id = :clienteId"
            + " AND r.fechaHora < :fin AND (r.fechaHora + COALESCE(r.servicio.duracionMinutos, 60) minute) > :inicio"
            + " AND (:excluirId IS NULL OR r.id <> :excluirId)")
    List<Reserva> buscarConflictosCliente(@Param("clienteId") Long clienteId,
                                          @Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin,
                                          @Param("excluirId") Long excluirId);
    boolean existsByEmpleadoIdAndClienteId(Long empleadoId, Long clienteId);
    boolean existsByEmpleadoIdAndClienteIdAndEstadoIn(Long empleadoId, Long clienteId, java.util.Collection<EstadoReserva> estados);
    long countByEstado(EstadoReserva estado);
    long countByClienteIdAndObservacionAndCanceladaEnGreaterThanEqual(Long clienteId, String observacion, LocalDateTime desde);

    @Query("SELECT r FROM Reserva r WHERE r.empleado.id = :empleadoId ORDER BY r.fechaHora DESC")
    List<Reserva> buscarPorEmpleado(@Param("empleadoId") Long empleadoId);
    boolean existsByServicioId(Long servicioId);
    boolean existsByEmpleadoId(Long empleadoId);
    boolean existsBySalaId(Long salaId);

    boolean existsByServicioIdAndEstadoIn(Long servicioId, java.util.Collection<EstadoReserva> estados);
    boolean existsByEmpleadoIdAndEstadoIn(Long empleadoId, java.util.Collection<EstadoReserva> estados);
    boolean existsBySalaIdAndEstadoIn(Long salaId, java.util.Collection<EstadoReserva> estados);

    @Query("SELECT r FROM Reserva r WHERE r.cliente.id = :clienteId ORDER BY r.fechaHora DESC")
    List<Reserva> buscarPorCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT r FROM Reserva r WHERE r.fechaHora BETWEEN :inicio AND :fin AND r.estado = :estado")
    List<Reserva> buscarPorRangoYEstado(@Param("inicio") LocalDateTime inicio,
                                         @Param("fin") LocalDateTime fin,
                                         @Param("estado") EstadoReserva estado);

    /**
     * Reservas activas de la misma sala o del mismo especialista cuyo bloque se solapa con [inicio, fin).
     * Dos bloques se solapan si inicio1 &lt; fin2 y inicio2 &lt; fin1.
     */
    @Query("SELECT r FROM Reserva r WHERE " + ACTIVA
            + " AND (r.sala.id = :salaId OR r.empleado.id = :empleadoId)"
            + " AND r.fechaHora < :fin AND " + FIN_BLOQUE + " > :inicio"
            + " AND (:excluirId IS NULL OR r.id <> :excluirId)")
    List<Reserva> buscarConflictos(@Param("salaId") Long salaId,
                                   @Param("empleadoId") Long empleadoId,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin,
                                   @Param("excluirId") Long excluirId);

    /** Agenda futura de un especialista, ordenada cronológicamente. */
    @Query("SELECT r FROM Reserva r WHERE r.empleado.id = :empleadoId AND r.fechaHora >= :desde "
            + "ORDER BY r.fechaHora ASC")
    List<Reserva> buscarAgendaEmpleado(@Param("empleadoId") Long empleadoId, @Param("desde") LocalDateTime desde);

    /** Reservas activas (PENDIENTE o CONFIRMADA) cuyo inicio ya pasó: candidatas a expirar, completarse o marcarse como no asistidas. */
    @Query("SELECT r FROM Reserva r WHERE " + ACTIVA + " AND r.fechaHora < :ahora")
    List<Reserva> buscarActivasIniciadasAntesDe(@Param("ahora") LocalDateTime ahora);

    /** Reservas cuyo inicio cae en [desde, hasta), en orden cronológico (base del estado en vivo y de los KPI). */
    @Query("SELECT r FROM Reserva r WHERE r.fechaHora >= :desde AND r.fechaHora < :hasta ORDER BY r.fechaHora ASC")
    List<Reserva> buscarEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
