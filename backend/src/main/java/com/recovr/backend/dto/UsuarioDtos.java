package com.recovr.backend.dto;

import com.recovr.backend.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contratos de la gestión de accesos y del cambio de contraseña. */
public final class UsuarioDtos {

    private UsuarioDtos() {}

    public record UsuarioResponse(Long id, String correo, String nombre, String rol, boolean activo,
                                  boolean debeCambiarClave, String vinculo, Long empleadoId) {}

    public record NuevoUsuarioRequest(
            @NotBlank @Size(min = 3, max = 120) String nombre,
            @NotBlank @Email String correo,
            @NotNull Rol rol,
            Long empleadoId) {}

    /** La clave temporal se muestra una sola vez; el usuario deberá cambiarla en su primer ingreso. */
    public record UsuarioNuevoResponse(UsuarioResponse usuario, String claveTemporal) {}

    public record CambiarClaveRequest(
            @NotBlank String claveActual,
            @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe combinar letras y números")
            String claveNueva) {}
}
