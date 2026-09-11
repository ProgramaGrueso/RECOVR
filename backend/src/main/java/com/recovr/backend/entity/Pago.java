package com.recovr.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Schema(description = "Entidad que registra la transacción de cobro de una reserva")
public class Pago {

    @Schema(description = "Identificador único de la transacción", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Reserva asociada al pago")
    @JsonIgnoreProperties({"pago"})
    @OneToOne
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    @Schema(description = "Monto abonado en la transacción", example = "60.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false)
    private BigDecimal monto;

    @Schema(description = "Método de pago utilizado", example = "TARJETA")
    @Column(name = "metodo_pago")
    private String metodoPago;

    @Schema(description = "Fecha y hora del registro del pago", example = "2026-10-15T10:15:00")
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    public Pago() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}
