package com.recovr.backend.repository;

import com.recovr.backend.entity.FichaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FichaClienteRepository extends JpaRepository<FichaCliente, Long> {
    Optional<FichaCliente> findByClienteId(Long clienteId);
}
