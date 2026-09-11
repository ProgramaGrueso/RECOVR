package com.recovr.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "servicios")
@Schema(description = "Entidad que representa un servicio o terapia ofrecida en RECOVR")
public class Servicio {

    @Schema(description = "Identificador único del servicio", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Nombre comercial del tratamiento o terapia", example = "Crioterapia de Cuerpo Entero", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false)
    private String nombre;

    @Schema(description = "Duración de la sesión en minutos", example = "45")
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Schema(description = "Tiempo de desinfección y preparación en minutos", example = "15")
    @Column(name = "tiempo_limpieza_minutos")
    private Integer tiempoLimpiezaMinutos;

    @Schema(description = "Precio del servicio", example = "60.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false)
    private BigDecimal precio;

    @JsonIgnore
    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL)
    private List<Reserva> reservas;

    public Servicio() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public Integer getTiempoLimpiezaMinutos() { return tiempoLimpiezaMinutos; }
    public void setTiempoLimpiezaMinutos(Integer tiempoLimpiezaMinutos) { this.tiempoLimpiezaMinutos = tiempoLimpiezaMinutos; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public List<Reserva> getReservas() { return reservas; }
    public void setReservas(List<Reserva> reservas) { this.reservas = reservas; }
}
