package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de la sesión actual, sin información sensible")
public record PerfilResponse(String correo, String rol, String nombre, Long clienteId, Long empleadoId, String telefono) {
}
