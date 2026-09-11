package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para inicio de sesión")
public record LoginRequest(
        @Schema(description = "Correo electrónico registrado", example = "admin@recovr.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String correo,

        @Schema(description = "Contraseña del usuario", example = "Admin123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password
) {}
