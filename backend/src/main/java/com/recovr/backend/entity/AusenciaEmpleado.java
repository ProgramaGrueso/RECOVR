package com.recovr.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Periodo en que una terapeuta no atiende (vacaciones, licencia, capacitación). Mientras dura, no recibe reservas.
 */
@Entity
@Table(name = "ausencias_empleado")
public class AusenciaEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private LocalDateTime desde;

    @Column(nullable = false)
    private LocalDateTime hasta;

    @Column(length = 150)
    private String motivo;

    public AusenciaEmpleado() {}

    public Long getId() { return id; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public LocalDateTime getDesde() { return desde; }
    public void setDesde(LocalDateTime desde) { this.desde = desde; }
    public LocalDateTime getHasta() { return hasta; }
    public void setHasta(LocalDateTime hasta) { this.hasta = hasta; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
