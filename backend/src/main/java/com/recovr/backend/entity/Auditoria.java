package com.recovr.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Registro de auditoría: quién hizo qué, desde dónde y con qué resultado. Nunca guarda cuerpos ni contraseñas. */
@Entity
@Table(name = "auditoria", indexes = @Index(name = "idx_auditoria_fecha", columnList = "fecha"))
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 120)
    private String usuario;

    @Column(length = 20)
    private String rol;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(length = 255)
    private String recurso;

    private Integer estado;

    @Column(length = 60)
    private String ip;

    @Column(length = 255)
    private String detalle;

    public Auditoria() {}

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public Integer getEstado() { return estado; }
    public void setEstado(Integer estado) { this.estado = estado; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
