package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Datos de un pago. reservaId solo se usa en POST /api/pagos; en confirmar-y-pagar se toma de la ruta")
public record PagoRequest(
        @Schema(description = "ID de la reserva pagada", example = "1")
        Long reservaId,

        @Schema(description = "Monto abonado", example = "120.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive BigDecimal monto,

        @Schema(description = "Método de pago", example = "TARJETA", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String metodoPago
) {}
