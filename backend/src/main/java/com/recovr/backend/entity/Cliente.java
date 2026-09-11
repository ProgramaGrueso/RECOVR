package com.recovr.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "clientes")
@Schema(description = "Entidad que representa a un cliente del centro de recuperación")
public class Cliente {

    @Schema(description = "Identificador único asignado automáticamente", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Nombre completo del cliente", example = "Ana Cliente", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false)
    private String nombre;

    @Schema(description = "Correo electrónico único del cliente", example = "ana@recovr.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false, unique = true)
    private String correo;

    @Schema(description = "Teléfono de contacto", example = "988888888")
    private String telefono;

    // Referencia al usuario de Auth. Se mantiene como ID para evitar ciclos de serializacion JSON.
    @Schema(description = "Identificador del usuario de autenticación vinculado", accessMode = Schema.AccessMode.READ_ONLY, example = "2")
    @Column(name = "usuario_id")
    private Long usuarioId;

    @JsonIgnore
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Reserva> reservas;

    public Cliente() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public List<Reserva> getReservas() { return reservas; }
    public void setReservas(List<Reserva> reservas) { this.reservas = reservas; }
}
