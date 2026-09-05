package com.recovr.repository;

import com.recovr.model.Reserva;
import java.util.List;
import java.util.Optional;

/**
 * Puerto / Contrato de persistencia para el módulo Core Engine.
 * Permite desacoplar las reglas de negocio de la implementación de base de datos (JPA/MySQL).
 */
public interface ReservaRepository {

    Reserva save(Reserva reserva);

    Optional<Reserva> findById(Long id);

    List<Reserva> findAll();

    List<Reserva> findByEmpleadoId(Long empleadoId);

    List<Reserva> findBySalaId(Long salaId);
}
