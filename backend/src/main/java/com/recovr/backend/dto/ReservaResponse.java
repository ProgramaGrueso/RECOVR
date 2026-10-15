package com.recovr.backend.dto;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.service.ReglasReserva;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
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
        @Schema(description = "Estado guardado: PENDIENTE, CONFIRMADA, COMPLETADA o CANCELADA") EstadoReserva estado,
        @Schema(description = "Estado para el personal: EN_ESPERA, CONFIRMADA, EN_SESION, COMPLETADA o CANCELADA") String estadoOperativo,
        @Schema(description = "Momento del check-in (nulo si el cliente aún no llegó)") LocalDateTime checkIn,
        @Schema(description = "Motivo del último cambio automático o manual de estado") String observacion,
        @Schema(description = "Precio del servicio") BigDecimal precio,
        @Schema(description = "Si tiene un pago registrado") boolean pagado,
        @Schema(description = "Método del pago registrado") String metodoPago
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
                r.getEstado(), ReglasReserva.estadoOperativo(r), r.getCheckIn(), r.getObservacion(),
                s.getPrecio(), r.getPago() != null, r.getPago() != null ? r.getPago().getMetodoPago() : null);
    }

    /** Fin del bloque: inicio + duración (60 min por defecto) + limpieza. Misma regla que la consulta JPQL. */
    public static LocalDateTime finBloque(LocalDateTime inicio, Servicio s) {
        int duracion = s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 60;
        int limpieza = s.getTiempoLimpiezaMinutos() != null ? s.getTiempoLimpiezaMinutos() : 0;
        return inicio.plusMinutes(duracion + limpieza);
    }
}
