package com.recovr.backend.dto;

import com.recovr.backend.entity.Empleado;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos públicos de un especialista")
public record EmpleadoResponse(Long id, String nombre, String especialidad, String telefono, Long usuarioId, boolean activo, String bio) {
    public static EmpleadoResponse de(Empleado e) {
        return new EmpleadoResponse(e.getId(), e.getNombre(), e.getEspecialidad(), e.getTelefono(), e.getUsuarioId(), e.isActivo(), e.getBio());
    }
}
