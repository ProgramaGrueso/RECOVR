package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estado en vivo de una cabina, calculado a partir de las reservas")
public record SalaEstadoResponse(
        Long id, String nombre, Integer capacidad,
        @Schema(description = "DISPONIBLE, OCUPADA, ESPERANDO_CLIENTE o EN_SANITIZACION") String estado,
        @Schema(description = "Terapeuta de la sesión en curso o de la que acaba de terminar") String terapeuta,
        @Schema(description = "Hasta cuándo dura el estado actual (fin de sesión o de limpieza)") LocalDateTime hasta,
        @Schema(description = "Inicio de la próxima reserva activa en esta cabina") LocalDateTime proximaReserva) {
}
