package com.recovr.repository;

import com.recovr.model.Reserva;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Adaptador / Implementación en memoria del repositorio de reservas.
 * Permite ejecutar y probar la lógica de negocio del Core Engine
 * sin invadir las tareas de JPA/Base de Datos asignadas al Integrante 4.
 */
@Repository
public class InMemoryReservaRepository implements ReservaRepository {

    private final Map<Long, Reserva> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Reserva save(Reserva reserva) {
        if (reserva.getId() == null) {
            reserva.setId(idGenerator.getAndIncrement());
        }
        storage.put(reserva.getId(), reserva);
        return reserva;
    }

    @Override
    public Optional<Reserva> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Reserva> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Reserva> findByEmpleadoId(Long empleadoId) {
        return storage.values().stream()
                .filter(r -> r.getEmpleadoId().equals(empleadoId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reserva> findBySalaId(Long salaId) {
        return storage.values().stream()
                .filter(r -> r.getSalaId().equals(salaId))
                .collect(Collectors.toList());
    }
}
