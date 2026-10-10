package com.recovr.backend.dto;

import com.recovr.backend.entity.Empleado;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Terapeuta visible para quien reserva (sin datos de contacto)")
public record EmpleadoCatalogoResponse(Long id, String nombre, String especialidad) {
    public static EmpleadoCatalogoResponse de(Empleado e) {
        return new EmpleadoCatalogoResponse(e.getId(), e.getNombre(), e.getEspecialidad());
    }
}
