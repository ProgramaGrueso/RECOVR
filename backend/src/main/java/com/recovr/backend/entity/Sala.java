package com.recovr.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "salas")
@Schema(description = "Entidad que representa una suite o cabina de recuperación")
public class Sala {

    @Schema(description = "Identificador único de la sala", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Nombre o identificador de la cabina", example = "Cabina Crio 1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false)
    private String nombre;

    @Schema(description = "Capacidad simultánea de usuarios", example = "2")
    private Integer capacidad;

    @JsonIgnore
    @OneToMany(mappedBy = "sala", cascade = CascadeType.ALL)
    private List<Reserva> reservas;

    public Sala() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public List<Reserva> getReservas() { return reservas; }
    public void setReservas(List<Reserva> reservas) { this.reservas = reservas; }
}
