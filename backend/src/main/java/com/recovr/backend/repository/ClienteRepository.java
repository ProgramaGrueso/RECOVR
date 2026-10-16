package com.recovr.backend.repository;

import com.recovr.backend.entity.Cliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuarioId(Long usuarioId);

    boolean existsByCorreoEndingWith(String sufijo);
    Optional<Cliente> findByCorreo(String correo);

    /** Lee la fila bloqueándola hasta el fin de la transacción: serializa las operaciones concurrentes sobre ella. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Cliente c WHERE c.id = :id")
    java.util.Optional<Cliente> buscarBloqueado(@org.springframework.data.repository.query.Param("id") Long id);
}
