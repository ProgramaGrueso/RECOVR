package com.recovr.backend.repository;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT r FROM Reserva r WHERE r.cliente.id = :clienteId ORDER BY r.fechaHora DESC")
    List<Reserva> buscarPorCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT r FROM Reserva r WHERE r.fechaHora BETWEEN :inicio AND :fin AND r.estado = :estado")
    List<Reserva> buscarPorRangoYEstado(@Param("inicio") LocalDateTime inicio,
                                         @Param("fin") LocalDateTime fin,
                                         @Param("estado") EstadoReserva estado);
}
