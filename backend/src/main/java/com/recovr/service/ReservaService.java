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
     * Paso GREEN de TDD: Implementación mínima para satisfacer el cálculo del bloque.
     * Suma la duración del servicio más el tiempo de limpieza e higienización de la sala.
     */
    public LocalDateTime calcularFinBloque(LocalDateTime inicio, Servicio servicio) {
        int minutosTotales = servicio.getDuracionMinutos();
        if (servicio.getTiempoLimpiezaMinutos() != null) {
            minutosTotales += servicio.getTiempoLimpiezaMinutos();
        }
        return inicio.plusMinutes(minutosTotales);
    }
}
