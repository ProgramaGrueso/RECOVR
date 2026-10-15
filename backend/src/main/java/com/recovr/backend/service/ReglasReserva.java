package com.recovr.backend.service;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reglas de negocio del ciclo de vida de una reserva, en un solo lugar para que el servicio, el planificador
 * y las respuestas de la API usen exactamente las mismas.
 *
 * <pre>
 *   PENDIENTE ──(pago / confirmar)──▶ CONFIRMADA ──(check-in)──▶ EN SESIÓN ──(fin de sesión)──▶ COMPLETADA
 *       │                                 │
 *       └──(cancelar o expirar)──▶ CANCELADA ◀──(cancelar o no asistir)
 * </pre>
 * "EN SESIÓN" no es un valor guardado: es CONFIRMADA con check-in. Así el esquema de la base no cambia.
 */
public final class ReglasReserva {

    /** Minutos después del inicio que se espera la confirmación (pago) de una reserva PENDIENTE antes de expirarla. */
    public static final int TOLERANCIA_CONFIRMACION_MIN = 15;
    /** Minutos después del inicio que se espera al cliente de una reserva CONFIRMADA antes de darla por no asistida. */
    public static final int TOLERANCIA_NO_SHOW_MIN = 30;
    /** Con cuántos minutos de anticipación se permite registrar el check-in. */
    public static final int CHECKIN_ANTICIPADO_MIN = 30;
    /** Horario de atención usado para calcular la ocupación diaria. */
    public static final LocalTime APERTURA = LocalTime.of(12, 0);
    public static final LocalTime CIERRE = LocalTime.of(23, 0);

    public static final String EN_ESPERA = "EN_ESPERA";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String EN_SESION = "EN_SESION";
    public static final String COMPLETADA = "COMPLETADA";
    public static final String CANCELADA = "CANCELADA";

    private ReglasReserva() {}

    public static int duracion(Reserva r) {
        Integer d = r.getServicio().getDuracionMinutos();
        return d != null ? d : 60;
    }

    public static int limpieza(Reserva r) {
        Integer l = r.getServicio().getTiempoLimpiezaMinutos();
        return l != null ? l : 0;
    }

    /** Fin de la sesión (sin limpieza). */
    public static LocalDateTime finSesion(Reserva r) {
        return r.getFechaHora().plusMinutes(duracion(r));
    }

    /** Fin del bloque ocupado: sesión + limpieza de la cabina. */
    public static LocalDateTime finBloque(Reserva r) {
        return finSesion(r).plusMinutes(limpieza(r));
    }

    public static boolean activa(Reserva r) {
        return r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.CONFIRMADA;
    }

    /** Estado que se muestra al personal: el guardado más el derivado "EN SESIÓN". */
    public static String estadoOperativo(Reserva r) {
        return switch (r.getEstado()) {
            case PENDIENTE -> EN_ESPERA;
            case CONFIRMADA -> r.getCheckIn() != null ? EN_SESION : CONFIRMADA;
            case COMPLETADA -> COMPLETADA;
            case CANCELADA -> CANCELADA;
        };
    }

    /** Transiciones permitidas entre estados guardados. COMPLETADA y CANCELADA son finales. */
    public static boolean transicionValida(EstadoReserva desde, EstadoReserva hacia) {
        if (desde == hacia) {
            return true;
        }
        return switch (desde) {
            case PENDIENTE -> hacia == EstadoReserva.CONFIRMADA || hacia == EstadoReserva.CANCELADA;
            case CONFIRMADA -> hacia == EstadoReserva.COMPLETADA || hacia == EstadoReserva.CANCELADA;
            case COMPLETADA, CANCELADA -> false;
        };
    }
}
