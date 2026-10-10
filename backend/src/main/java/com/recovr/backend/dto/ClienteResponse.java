package com.recovr.backend.dto;

import com.recovr.backend.entity.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos públicos de un cliente")
public record ClienteResponse(Long id, String nombre, String correo, String telefono, Long usuarioId) {
    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getCorreo(), c.getTelefono(), c.getUsuarioId());
    }
}
