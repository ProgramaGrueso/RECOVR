package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para el registro de un nuevo usuario")
public record RegistroRequest(
        @Schema(description = "Nombre completo del usuario", example = "Ana Cliente", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String nombre,

        @Schema(description = "Correo electrónico del usuario", example = "ana@recovr.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String correo,

        @Schema(description = "Contraseña segura (mínimo 6 caracteres)", example = "Cliente123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 6) String password,

        @Schema(description = "Número telefónico de contacto", example = "988888888")
        String telefono
) {}
