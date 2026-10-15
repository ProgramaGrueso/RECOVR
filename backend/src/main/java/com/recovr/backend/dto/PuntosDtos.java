package com.recovr.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Contratos del programa de puntos. Todo es derivable de las reservas y los canjes: no hay saldos inventados. */
public final class PuntosDtos {

    private PuntosDtos() {}

    public record Movimiento(LocalDateTime fecha, String tipo, String descripcion, int puntos) {}

    public record BeneficioInfo(String id, String titulo, String descripcion, int puntos, BigDecimal descuento, boolean alcanza) {}

    public record NivelPuntos(String nombre, int desde, boolean actual) {}

    public record CanjeInfo(String codigo, String titulo, BigDecimal descuento, String estado, LocalDateTime creadoEn, LocalDateTime usadoEn) {}

    public record PuntosResponse(int saldo, int ganados, int canjeados, String nivel, String siguienteNivel, int puntosParaSiguiente,
                                 List<String> reglas, List<NivelPuntos> niveles, List<BeneficioInfo> beneficios,
                                 List<Movimiento> movimientos, List<CanjeInfo> canjes) {}

    public record CanjearRequest(@NotBlank String beneficioId) {}

    /** Vista previa para recepción: qué monto corresponde cobrar si se aplica un código a una reserva. */
    public record CanjeValidoResponse(String codigo, String titulo, String cliente, BigDecimal descuento, String estado,
                                      boolean valido, String motivo, BigDecimal precio, BigDecimal montoACobrar) {}
}
