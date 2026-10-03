package com.recovr.backend.repository;

import com.recovr.backend.entity.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    /**
     * Salas sin reservas activas cuyo bloque (duración + limpieza) se solape con [inicio, fin).
     */
    @Query("SELECT s FROM Sala s WHERE NOT EXISTS (" +
           "SELECT r FROM Reserva r WHERE r.sala = s AND " + ReservaRepository.ACTIVA +
           " AND r.fechaHora < :fin AND " + ReservaRepository.FIN_BLOQUE + " > :inicio)")
    List<Sala> buscarDisponibles(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
