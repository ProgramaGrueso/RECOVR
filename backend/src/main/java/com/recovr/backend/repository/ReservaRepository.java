package com.recovr.backend.repository;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}
