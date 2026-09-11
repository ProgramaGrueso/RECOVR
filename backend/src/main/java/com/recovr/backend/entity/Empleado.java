package com.recovr.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "empleados")
@Schema(description = "Entidad que representa a un terapeuta o especialista de recuperación")
public class Empleado {

    @Schema(description = "Identificador único del empleado", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Nombre completo del especialista", example = "Carlos Fisioterapeuta", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false)
    private String nombre;

    @Schema(description = "Área de especialidad o rol clínico", example = "Crioterapia y Recuperación Muscular")
    private String especialidad;

    @Schema(description = "Teléfono de contacto", example = "911222333")
    private String telefono;

    @JsonIgnore
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL)
    private List<Reserva> reservas;

    public Empleado() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public List<Reserva> getReservas() { return reservas; }
    public void setReservas(List<Reserva> reservas) { this.reservas = reservas; }
}
