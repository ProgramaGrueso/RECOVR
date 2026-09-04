package com.recovr.service;

import com.recovr.model.Servicio;
import com.recovr.repository.InMemoryReservaRepository;
import com.recovr.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReservaServiceTest {

    private ReservaService reservaService;
    private ReservaRepository reservaRepository;

    @BeforeEach
    void setUp() {
        reservaRepository = new InMemoryReservaRepository();
        reservaService = new ReservaService(reservaRepository);
    }

    @Test
    @DisplayName("Debe calcular la hora de finalización del bloque sumando duración del servicio y tiempo de limpieza")
    void debeCalcularHoraFinBloqueSumandoDuracionYLimpieza() {
        // Given: Un servicio de Descompresión Miofascial (75 min) con 15 min de limpieza de sala
        Servicio servicio = new Servicio(
                1L,
                "Descompresión Miofascial Profunda",
                75,
                15,
                new BigDecimal("65.00")
        );
        LocalDateTime inicio = LocalDateTime.of(2026, 9, 10, 10, 0);

        // When: Calculamos la hora de fin de ocupación del bloque (75 + 15 = 90 minutos)
        LocalDateTime finBloque = reservaService.calcularFinBloque(inicio, servicio);

        // Then: Debe finalizar exactamente a las 11:30 (10:00 + 90 min)
        assertNotNull(finBloque, "El fin del bloque no debe ser nulo");
        assertEquals(LocalDateTime.of(2026, 9, 10, 11, 30), finBloque);
    }
}
