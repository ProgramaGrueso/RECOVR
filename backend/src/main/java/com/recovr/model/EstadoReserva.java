package com.recovr.model;

/**
 * Estados permitidos para una reserva en RECOVR.
 * Coherente con el esquema oficial de base de datos MySQL (columna ENUM).
 */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    COMPLETADA
}
