package com.recovr.backend.dto;

import com.recovr.backend.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank @Email String correo,
        @NotBlank @Size(min = 6) String password,
        @NotNull Rol rol
) {}
