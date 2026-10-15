package com.recovr.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class MiCuentaDtos {

    private MiCuentaDtos() {}

    public record ActualizarPerfilRequest(@NotBlank @Size(min = 3, max = 120) String nombre, @Size(max = 20) String telefono) {}

    public record NivelInfo(String nivel, int desde) {}

    public record MiCuentaResponse(
            String nombre, String correo, String telefono,
            int puntos, String nivel, String siguienteNivel, int puntosParaSiguiente,
            long sesionesCompletadas, ReservaResponse proximaReserva,
            long cancelacionesMes, int limiteCancelacionesMes, int horasMinimasCancelacion,
            LocalDateTime generadoEn) {}
}
