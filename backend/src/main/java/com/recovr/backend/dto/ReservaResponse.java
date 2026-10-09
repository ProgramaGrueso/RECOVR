package com.recovr.backend.dto;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Servicio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Reserva persistida con sus datos relacionados aplanados")
public record ReservaResponse(
        Long id,
        Long clienteId, String clienteNombre,
        Long empleadoId, String empleadoNombre,
        Long servicioId, String servicioNombre,
        Long salaId, String salaNombre,
        @Schema(description = "Inicio de la sesión") LocalDateTime fechaHora,
        @Schema(description = "Fin del bloque ocupado (duración + limpieza)") LocalDateTime finBloque,
        EstadoReserva estado
) {
    public static ReservaResponse de(Reserva r) {
        Servicio s = r.getServicio();
        return new ReservaResponse(
                r.getId(),
                r.getCliente().getId(), r.getCliente().getNombre(),
                r.getEmpleado().getId(), r.getEmpleado().getNombre(),
                s.getId(), s.getNombre(),
                r.getSala().getId(), r.getSala().getNombre(),
                r.getFechaHora(), finBloque(r.getFechaHora(), s),
                r.getEstado());
    }

    /** Fin del bloque: inicio + duración (60 min por defecto) + limpieza. Misma regla que la consulta JPQL. */
    public static LocalDateTime finBloque(LocalDateTime inicio, Servicio s) {
        int duracion = s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 60;
        int limpieza = s.getTiempoLimpiezaMinutos() != null ? s.getTiempoLimpiezaMinutos() : 0;
        return inicio.plusMinutes(duracion + limpieza);
    }
}
