package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de autenticación exitosa con token JWT")
public record AuthResponse(
        @Schema(description = "Token de acceso JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Tipo de esquema de token", example = "Bearer")
        String tipo,

        @Schema(description = "Rol asignado al usuario", example = "ADMIN")
        String rol,

        @Schema(description = "Correo electrónico del usuario", example = "admin@recovr.com")
        String correo
) {}
