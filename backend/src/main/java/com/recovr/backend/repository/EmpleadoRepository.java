package com.recovr.backend.repository;

import com.recovr.backend.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
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
           " AND r.fechaHora < :fin AND " + ReservaRepository.FIN_BLOQUE + " > :inicio)")
    List<Empleado> buscarDisponibles(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    Optional<Empleado> findByUsuarioId(Long usuarioId);
}
