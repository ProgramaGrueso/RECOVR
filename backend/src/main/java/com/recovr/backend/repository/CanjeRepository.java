package com.recovr.backend.repository;

import com.recovr.backend.entity.Canje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CanjeRepository extends JpaRepository<Canje, Long> {
    Optional<Canje> findByCodigo(String codigo);

    List<Canje> findByClienteIdOrderByCreadoEnDesc(Long clienteId);

    @Query("SELECT COALESCE(SUM(c.puntos), 0) FROM Canje c WHERE c.cliente.id = :clienteId")
    long sumarPuntosCanjeados(@Param("clienteId") Long clienteId);
}
