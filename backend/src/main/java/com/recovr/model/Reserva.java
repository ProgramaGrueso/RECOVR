package com.recovr.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo de dominio puro para una Reserva en RECOVR.
 * Contiene los datos indispensables para las reglas de negocio del Core Engine.
 */
public class Reserva {

    private Long id;
    private Long clienteId;
    private Long empleadoId;
    private Long servicioId;
    private Long salaId;
    private LocalDateTime fechaHora;
    private EstadoReserva estado;
    private BigDecimal montoTotal;

    public Reserva() {
    }

    public Reserva(Long id, Long clienteId, Long empleadoId, Long servicioId, Long salaId,
                   LocalDateTime fechaHora, EstadoReserva estado, BigDecimal montoTotal) {
        this.id = id;
        this.clienteId = clienteId;
        this.empleadoId = empleadoId;
        this.servicioId = servicioId;
        this.salaId = salaId;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.montoTotal = montoTotal;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reserva reserva = (Reserva) o;
        return Objects.equals(id, reserva.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
