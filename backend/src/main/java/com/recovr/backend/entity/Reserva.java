package com.recovr.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Schema(description = "Entidad de base de datos que representa una reserva agendada")
public class Reserva {

    @Schema(description = "Identificador único de la reserva", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Cliente que contrató la sesión")
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Schema(description = "Especialista asignado al turno")
    @ManyToOne
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Schema(description = "Tratamiento o terapia reservada")
    @ManyToOne
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Schema(description = "Sala o cabina asignada")
    @ManyToOne
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Schema(description = "Fecha y hora del turno agendado", example = "2026-10-15T10:00:00")
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Schema(description = "Estado de la reserva", example = "PENDIENTE")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @JsonIgnore
    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Pago pago;

    public Reserva() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio servicio) { this.servicio = servicio; }

    public Sala getSala() { return sala; }
    public void setSala(Sala sala) { this.sala = sala; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }
}
