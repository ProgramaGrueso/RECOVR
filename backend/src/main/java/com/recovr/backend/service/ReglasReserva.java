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

    /**
     * Minutos que una reserva PENDIENTE retiene el cupo desde que se crea: si no se paga completa en ese plazo, expira
     * y el cupo se libera. Solo el pago total asegura el turno; así nadie puede acaparar la agenda sin pagar.
     */
    public static final int HOLD_PAGO_MIN = 15;
    /** Reservas PENDIENTES (sin pagar) que un mismo cliente puede tener a la vez. */
    public static final int MAX_PENDIENTES_CLIENTE = 1;
    /** Reservas expiradas sin pagar en {@value #VENTANA_EXPIRADAS_HORAS} h tras las cuales se bloquean nuevas reservas. */
    public static final int MAX_EXPIRADAS_VENTANA = 3;
    public static final int VENTANA_EXPIRADAS_HORAS = 24;
    /**
     * Minutos después del inicio que se espera el pago de una reserva PENDIENTE antigua, creada antes de existir el
     * campo {@code creadaEn} (sin fecha de creación no se puede medir el plazo desde ella).
     */
    public static final int TOLERANCIA_CONFIRMACION_MIN = 15;
    /** Minutos después del inicio que se espera al cliente de una reserva CONFIRMADA antes de darla por no asistida. */
    public static final int TOLERANCIA_NO_SHOW_MIN = 30;
    /** Con cuántos minutos de anticipación se permite registrar el check-in. */
    public static final int CHECKIN_ANTICIPADO_MIN = 30;
    /** Horario de atención usado para calcular la ocupación diaria. */
    public static final LocalTime APERTURA = LocalTime.of(12, 0);
    public static final LocalTime CIERRE = LocalTime.of(23, 0);

    /** Turnos que ofrece la reserva en línea, cada 90 min dentro del horario de atención (recepción puede usar otras horas). */
    public static final java.util.List<LocalTime> TURNOS = java.util.List.of(
            LocalTime.of(12, 0), LocalTime.of(13, 30), LocalTime.of(15, 0), LocalTime.of(16, 30),
            LocalTime.of(18, 0), LocalTime.of(19, 30), LocalTime.of(21, 0));

    /** Un cliente solo puede cancelar por su cuenta con al menos estas horas de anticipación. */
    public static final int HORAS_MIN_CANCELACION_CLIENTE = 4;
    /** Máximo de cancelaciones hechas por el propio cliente en el mes calendario. */
    public static final int MAX_CANCELACIONES_CLIENTE_MES = 2;
    public static final String CANCELADA_POR_CLIENTE = "Cancelada por el cliente.";
    public static final String CANCELADA_POR_PERSONAL = "Cancelada por el personal.";
    public static final String EXPIRADA_SIN_PAGO = "Expirada automáticamente: no se pagó a tiempo y el cupo se liberó.";
    /** Beneficio interno del crédito que se entrega al cliente cuando se cancela una sesión que ya había pagado. */
    public static final String CREDITO_CANCELACION = "credito-cancelacion";
    public static final String NO_ASISTIO = "No asistió: no hubo check-in dentro de la tolerancia.";
    /** Días de vigencia de un código canjeado con puntos y de un crédito por cancelación. */
    public static final int VIGENCIA_CANJE_DIAS = 90;
    public static final int VIGENCIA_CREDITO_DIAS = 180;

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

    /** ¿Venció el plazo de pago de una reserva PENDIENTE? (solo mide desde la creación si se conoce). */
    public static boolean holdVencido(Reserva r, LocalDateTime ahora) {
        return r.getEstado() == EstadoReserva.PENDIENTE && r.getCreadaEn() != null
                && !ahora.isBefore(r.getCreadaEn().plusMinutes(HOLD_PAGO_MIN));
    }

    /** La sesión completa debe empezar y terminar dentro del horario de atención y el mismo día. */
    public static boolean dentroDeHorario(LocalDateTime inicio, LocalDateTime finSesion) {
        return finSesion.toLocalDate().equals(inicio.toLocalDate())
                && !inicio.toLocalTime().isBefore(APERTURA) && !finSesion.toLocalTime().isAfter(CIERRE);
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
