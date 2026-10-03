package com.recovr.backend.dto;

import com.recovr.backend.entity.Sala;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "Datos para crear o actualizar una sala o cabina")
public record SalaRequest(
        @Schema(description = "Nombre de la sala", example = "Cabina Privada 1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String nombre,

        @Schema(description = "Capacidad de personas", example = "1")
        @Positive Integer capacidad
) {
    public Sala aEntidad() {
        Sala sala = new Sala();
        sala.setNombre(nombre);
        sala.setCapacidad(capacidad);
        return sala;
    }
}
