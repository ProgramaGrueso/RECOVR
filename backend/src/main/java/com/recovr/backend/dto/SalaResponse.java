package com.recovr.backend.dto;

import com.recovr.backend.entity.Sala;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de una sala o suite de tratamiento")
public record SalaResponse(Long id, String nombre, Integer capacidad, boolean activa) {
    public static SalaResponse de(Sala s) {
        return new SalaResponse(s.getId(), s.getNombre(), s.getCapacidad(), s.isActiva());
    }
}
