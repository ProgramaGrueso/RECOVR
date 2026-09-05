package com.recovr.backend.repository;

import com.recovr.backend.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @Query("SELECT e FROM Empleado e WHERE e.id NOT IN (" +
           "SELECT r.empleado.id FROM Reserva r " +
           "WHERE r.fechaHora BETWEEN :inicio AND :fin " +
           "AND r.estado <> com.recovr.backend.entity.EstadoReserva.CANCELADA)")
    List<Empleado> buscarDisponibles(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
