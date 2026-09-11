package com.recovr.backend.dto;

import com.recovr.backend.entity.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para que un administrador cree un usuario de personal (RECEPCIONISTA o ESPECIALISTA)")
public record CrearUsuarioRequest(
        @Schema(description = "Correo electrónico institucional", example = "recepcion@recovr.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String correo,

        @Schema(description = "Contraseña temporal o inicial (mínimo 6 caracteres)", example = "Recepcion123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 6) String password,

        @Schema(description = "Rol asignado dentro del sistema", example = "RECEPCIONISTA", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Rol rol
) {}
