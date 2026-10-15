package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Disponibilidad en vivo de una terapeuta, calculada a partir de las reservas")
public record EmpleadoEstadoResponse(
        Long id, String nombre, String especialidad, String telefono, boolean activo,
        @Schema(description = "LIBRE, EN_SESION o POR_INICIAR") String estado,
        @Schema(description = "Cabina de la sesión en curso") String sala,
        @Schema(description = "Fin de la sesión en curso") LocalDateTime hasta,
        @Schema(description = "Inicio de su próxima reserva activa") LocalDateTime proximaReserva) {
}
