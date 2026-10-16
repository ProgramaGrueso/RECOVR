package com.recovr.backend.repository;

import com.recovr.backend.entity.Sala;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    /**
     * Salas en servicio (no en mantenimiento) sin reservas activas cuyo bloque (duración + limpieza) se solape con [inicio, fin).
     */
    @Query("SELECT s FROM Sala s WHERE NOT EXISTS (" +
           "SELECT r FROM Reserva r WHERE r.sala = s AND " + ReservaRepository.ACTIVA +
           " AND r.fechaHora < :fin AND " + ReservaRepository.FIN_BLOQUE + " > :inicio)"
           + " AND (s.activa IS NULL OR s.activa = true)")
    List<Sala> buscarDisponibles(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /** Lee la fila bloqueándola hasta el fin de la transacción: serializa las operaciones concurrentes sobre ella. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Sala s WHERE s.id = :id")
    java.util.Optional<Sala> buscarBloqueada(@org.springframework.data.repository.query.Param("id") Long id);
}
