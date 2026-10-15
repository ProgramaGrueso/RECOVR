package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Indicadores del panel de control, calculados desde la base de datos")
public record ResumenResponse(
        @Schema(description = "Suma de pagos del mes en curso, sin contar reservas canceladas") BigDecimal ingresosMes,
        @Schema(description = "Minutos de sesión reservados hoy / (cabinas × minutos de atención), en %") double ocupacionHoyPct,
        @Schema(description = "Reservas COMPLETADA con inicio en el mes") long sesionesCompletadasMes,
        @Schema(description = "Reservas activas (PENDIENTE o CONFIRMADA) de hoy") long agendadasHoy,
        @Schema(description = "Reservas PENDIENTE que esperan confirmación") long pendientesPorConfirmar,
        long canceladasMes, long totalMes,
        @Schema(description = "canceladasMes / totalMes, en %") double tasaCancelacionPct) {
}
