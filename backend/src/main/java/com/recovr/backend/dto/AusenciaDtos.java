package com.recovr.backend.dto;

import com.recovr.backend.entity.AusenciaEmpleado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AusenciaDtos {

    private AusenciaDtos() {}

    @Schema(description = "Periodo en que la terapeuta no atiende")
    public record AusenciaRequest(
            @Schema(description = "Inicio de la ausencia", example = "2026-11-03T00:00:00") @NotNull LocalDateTime desde,
            @Schema(description = "Fin de la ausencia", example = "2026-11-10T00:00:00") @NotNull LocalDateTime hasta,
            @Schema(description = "Motivo", example = "Vacaciones") @Size(max = 150) String motivo) {}

    public record AusenciaResponse(Long id, Long empleadoId, LocalDateTime desde, LocalDateTime hasta, String motivo) {
        public static AusenciaResponse de(AusenciaEmpleado a) {
            return new AusenciaResponse(a.getId(), a.getEmpleado().getId(), a.getDesde(), a.getHasta(), a.getMotivo());
        }
    }
}
