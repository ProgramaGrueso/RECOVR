package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Un turno del día y si se puede reservar con esa terapeuta")
public record TurnoDisponibleResponse(
        @Schema(description = "Hora de inicio", example = "18:00") String hora,
        @Schema(description = "true si se puede reservar ahora mismo") boolean disponible,
        @Schema(description = "Por qué no está disponible (nulo si lo está)", example = "La terapeuta ya tiene una sesión") String motivo) {
}
