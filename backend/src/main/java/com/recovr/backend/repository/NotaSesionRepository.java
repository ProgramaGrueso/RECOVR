package com.recovr.backend.repository;

import com.recovr.backend.entity.NotaSesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotaSesionRepository extends JpaRepository<NotaSesion, Long> {
    List<NotaSesion> findByClienteIdOrderByFechaDesc(Long clienteId);
}
