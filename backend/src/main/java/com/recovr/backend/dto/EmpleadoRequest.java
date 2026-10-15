package com.recovr.backend.dto;

import com.recovr.backend.entity.Empleado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar un especialista")
public record EmpleadoRequest(
        @Schema(description = "Nombre completo", example = "Carlos Fisioterapeuta", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String nombre,

        @Schema(description = "Especialidad", example = "Crioterapia y Recuperación Muscular")
        String especialidad,

        @Schema(description = "Teléfono de contacto", example = "911222333")
        @Size(max = 20) String telefono,
        @Size(max = 500) String bio
) {
    public Empleado aEntidad() {
        Empleado empleado = new Empleado();
        empleado.setNombre(nombre);
        empleado.setEspecialidad(especialidad);
        empleado.setTelefono(telefono);
        empleado.setBio(bio);
        return empleado;
    }
}
