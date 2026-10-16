package com.recovr.backend.repository;

import com.recovr.backend.entity.Canje;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CanjeRepository extends JpaRepository<Canje, Long> {
    Optional<Canje> findByCodigo(String codigo);

    /** Igual que {@link #findByCodigo} pero bloquea la fila: un código no puede aplicarse en dos cobros a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Canje c WHERE c.codigo = :codigo")
    Optional<Canje> buscarPorCodigoBloqueado(@Param("codigo") String codigo);

    List<Canje> findByReservaId(Long reservaId);

    List<Canje> findByClienteIdOrderByCreadoEnDesc(Long clienteId);

    @Query("SELECT COALESCE(SUM(c.puntos), 0) FROM Canje c WHERE c.cliente.id = :clienteId AND c.estado <> com.recovr.backend.entity.Canje.Estado.ANULADO")
    long sumarPuntosCanjeados(@Param("clienteId") Long clienteId);
}
