package com.recovr.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Foto de perfil de una terapeuta, en tabla aparte para no cargar los bytes al listar empleados. */
@Entity
@Table(name = "fotos_empleado")
public class FotoEmpleado {

    @Id
    @Column(name = "empleado_id")
    private Long empleadoId;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Lob
    @Column(name = "datos", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] datos;

    @Column(name = "actualizada_en", nullable = false)
    private LocalDateTime actualizadaEn;

    public FotoEmpleado() {}

    public Long getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(Long empleadoId) { this.empleadoId = empleadoId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public byte[] getDatos() { return datos; }
    public void setDatos(byte[] datos) { this.datos = datos; }
    public LocalDateTime getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(LocalDateTime actualizadaEn) { this.actualizadaEn = actualizadaEn; }
}
