package com.recovr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) para la creación de una reserva.
 * Desacopla la interfaz pública REST del modelo interno de dominio.
 */
@Schema(description = "Solicitud para registrar una nueva reserva en RECOVR")
public class CrearReservaRequest {

    @Schema(description = "Identificador del cliente", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clienteId;

    @Schema(description = "Identificador del especialista", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long empleadoId;

    @Schema(description = "Identificador del servicio o tratamiento", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long servicioId;

    @Schema(description = "Identificador de la sala o cabina", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long salaId;

    @Schema(description = "Fecha y hora de inicio de la sesión", example = "2026-10-15T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaHora;

    @Schema(description = "Duración estimada en minutos", example = "60")
    private Integer duracionTotalMinutos;

    @Schema(description = "Monto total calculado", example = "55.00")
    private BigDecimal montoTotal;

    public CrearReservaRequest() {
    }

    public CrearReservaRequest(Long clienteId, Long empleadoId, Long servicioId, Long salaId,
                               LocalDateTime fechaHora, Integer duracionTotalMinutos, BigDecimal montoTotal) {
        this.clienteId = clienteId;
        this.empleadoId = empleadoId;
        this.servicioId = servicioId;
        this.salaId = salaId;
        this.fechaHora = fechaHora;
        this.duracionTotalMinutos = duracionTotalMinutos;
        this.montoTotal = montoTotal;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public void setEmpleadoId(Long empleadoId) {
        this.empleadoId = empleadoId;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public Long getSalaId() {
        return salaId;
    }

    public void setSalaId(Long salaId) {
        this.salaId = salaId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Integer getDuracionTotalMinutos() {
        return duracionTotalMinutos;
    }

    public void setDuracionTotalMinutos(Integer duracionTotalMinutos) {
        this.duracionTotalMinutos = duracionTotalMinutos;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    /**
     * Mapea el DTO a la entidad de dominio Reserva.
     * Mantiene los controladores desacoplados y delgados.
     */
    public com.recovr.model.Reserva toDomain() {
        return new com.recovr.model.Reserva(
                null,
                clienteId,
                empleadoId,
                servicioId,
                salaId,
                fechaHora,
                duracionTotalMinutos,
                null,
                montoTotal
        );
    }
}
