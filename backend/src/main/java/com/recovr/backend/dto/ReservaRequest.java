package com.recovr.backend.dto;

import com.recovr.backend.entity.EstadoReserva;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Datos para crear o actualizar una reserva persistida")
public record ReservaRequest(
        @Schema(description = "ID del cliente. Lo ignora el servidor si quien reserva es un CLIENTE (se usa el autenticado)", example = "1")
        Long clienteId,

        @Schema(description = "ID del especialista", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long empleadoId,

        @Schema(description = "ID del servicio", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long servicioId,

        @Schema(description = "ID de la sala", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long salaId,

        @Schema(description = "Fecha y hora de inicio (ISO-8601)", example = "2026-10-15T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDateTime fechaHora,

        @Schema(description = "Estado. Solo se considera en actualizaciones hechas por ADMIN o RECEPCIONISTA", example = "PENDIENTE")
        EstadoReserva estado
) {}
