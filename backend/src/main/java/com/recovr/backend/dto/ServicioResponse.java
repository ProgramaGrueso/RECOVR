package com.recovr.backend.dto;

import com.recovr.backend.entity.Servicio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Servicio o terapia del catálogo")
public record ServicioResponse(Long id, String nombre, Integer duracionMinutos,
                               Integer tiempoLimpiezaMinutos, BigDecimal precio) {
    public static ServicioResponse de(Servicio s) {
        return new ServicioResponse(s.getId(), s.getNombre(), s.getDuracionMinutos(),
                s.getTiempoLimpiezaMinutos(), s.getPrecio());
    }
}
