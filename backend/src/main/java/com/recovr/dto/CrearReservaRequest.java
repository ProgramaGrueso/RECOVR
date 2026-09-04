package com.recovr.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) para la creación de una reserva.
 * Desacopla la interfaz pública REST del modelo interno de dominio.
 */
public class CrearReservaRequest {

    private Long clienteId;
    private Long empleadoId;
    private Long servicioId;
    private Long salaId;
    private LocalDateTime fechaHora;
    private Integer duracionTotalMinutos;
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
