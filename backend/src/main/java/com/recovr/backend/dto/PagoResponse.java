package com.recovr.backend.dto;

import com.recovr.backend.entity.Pago;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Pago registrado para una reserva")
public record PagoResponse(Long id, Long reservaId, BigDecimal monto, String metodoPago, LocalDateTime fechaPago) {
    public static PagoResponse de(Pago p) {
        return new PagoResponse(p.getId(), p.getReserva().getId(), p.getMonto(), p.getMetodoPago(), p.getFechaPago());
    }
}
