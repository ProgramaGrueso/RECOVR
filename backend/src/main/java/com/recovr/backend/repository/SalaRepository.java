package com.recovr.backend.repository;

import com.recovr.backend.entity.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    @Query("SELECT s FROM Sala s WHERE s.id NOT IN (" +
           "SELECT r.sala.id FROM Reserva r " +
           "WHERE r.fechaHora BETWEEN :inicio AND :fin " +
           "AND r.estado <> com.recovr.backend.entity.EstadoReserva.CANCELADA)")
    List<Sala> buscarDisponibles(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
