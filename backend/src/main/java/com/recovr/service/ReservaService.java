package com.recovr.service;

import com.recovr.model.Servicio;
import com.recovr.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio central del Core Engine para gestionar la lógica de reservas.
 * Demuestra Dependency Injection mediante constructor injection.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;

    public ReservaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /**
     * Paso RED de TDD: Definición del método sin implementación real.
     * Retorna null deliberadamente para validar que el test falla por la razón esperada.
     */
    public LocalDateTime calcularFinBloque(LocalDateTime inicio, Servicio servicio) {
        return null;
    }
}
