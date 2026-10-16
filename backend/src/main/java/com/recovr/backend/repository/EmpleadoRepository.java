package com.recovr.backend.repository;

import com.recovr.backend.entity.Empleado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    /**
     * Especialistas sin reservas activas cuyo bloque (duración + limpieza) se solape con [inicio, fin).
     */
    @Query("SELECT e FROM Empleado e WHERE NOT EXISTS (" +
           "SELECT r FROM Reserva r WHERE r.empleado = e AND " + ReservaRepository.ACTIVA +
           " AND r.fechaHora < :fin AND " + ReservaRepository.FIN_BLOQUE + " > :inicio)"
           + " AND (e.activo IS NULL OR e.activo = true)"
           + " AND NOT EXISTS (SELECT a FROM AusenciaEmpleado a WHERE a.empleado = e AND a.desde < :fin AND a.hasta > :inicio)")
    List<Empleado> buscarDisponibles(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    Optional<Empleado> findByUsuarioId(Long usuarioId);

    /** Lee la fila bloqueándola hasta el fin de la transacción: serializa las operaciones concurrentes sobre ella. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Empleado e WHERE e.id = :id")
    java.util.Optional<Empleado> buscarBloqueado(@org.springframework.data.repository.query.Param("id") Long id);
}
