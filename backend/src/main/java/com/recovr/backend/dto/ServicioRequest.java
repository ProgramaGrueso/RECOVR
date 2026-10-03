package com.recovr.backend.dto;

import com.recovr.backend.entity.Servicio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Datos para crear o actualizar un servicio del catálogo")
public record ServicioRequest(
        @Schema(description = "Nombre de la terapia", example = "Crioterapia de cuerpo entero", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String nombre,

        @Schema(description = "Duración de la sesión en minutos", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive @Max(480) Integer duracionMinutos,

        @Schema(description = "Minutos de limpieza de la sala tras la sesión", example = "15")
        @PositiveOrZero @Max(120) Integer tiempoLimpiezaMinutos,

        @Schema(description = "Precio en soles", example = "120.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive BigDecimal precio
) {
    public Servicio aEntidad() {
        Servicio servicio = new Servicio();
        servicio.setNombre(nombre);
        servicio.setDuracionMinutos(duracionMinutos);
        servicio.setTiempoLimpiezaMinutos(tiempoLimpiezaMinutos == null ? 0 : tiempoLimpiezaMinutos);
        servicio.setPrecio(precio);
        return servicio;
    }
}
