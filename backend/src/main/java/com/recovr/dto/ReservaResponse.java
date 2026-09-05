package com.recovr.dto;

import com.recovr.model.EstadoReserva;
import com.recovr.model.Reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) para la respuesta de una reserva.
 * Proporciona una vista pública y segura de la reserva confirmada.
 */
public class ReservaResponse {

    private Long id;
    private Long clienteId;
    private Long empleadoId;
    private Long servicioId;
    private Long salaId;
    private LocalDateTime fechaHora;
    private Integer duracionTotalMinutos;
    private EstadoReserva estado;
    private BigDecimal montoTotal;

    public ReservaResponse() {
    }

    public ReservaResponse(Long id, Long clienteId, Long empleadoId, Long servicioId, Long salaId,
                           LocalDateTime fechaHora, Integer duracionTotalMinutos, EstadoReserva estado, BigDecimal montoTotal) {
        this.id = id;
        this.clienteId = clienteId;
        this.empleadoId = empleadoId;
        this.servicioId = servicioId;
        this.salaId = salaId;
        this.fechaHora = fechaHora;
        this.duracionTotalMinutos = duracionTotalMinutos;
        this.estado = estado;
        this.montoTotal = montoTotal;
    }

    public static ReservaResponse fromDomain(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getClienteId(),
                reserva.getEmpleadoId(),
                reserva.getServicioId(),
                reserva.getSalaId(),
                reserva.getFechaHora(),
                reserva.getDuracionTotalMinutos(),
                reserva.getEstado(),
                reserva.getMontoTotal()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }
}
