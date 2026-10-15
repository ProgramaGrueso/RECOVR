package com.recovr.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(length = 120)
    private String nombre;

    /** false = cuenta desactivada: conserva su historial pero no puede iniciar sesión ni usar un token anterior. */
    @Column(name = "activo")
    private Boolean activo = Boolean.TRUE;

    /** true = clave temporal: hasta cambiarla solo se permite /auth/me y /auth/cambiar-clave. */
    @Column(name = "debe_cambiar_clave")
    private Boolean debeCambiarClave = Boolean.FALSE;

    public Usuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActivo() { return activo == null || activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean isDebeCambiarClave() { return debeCambiarClave != null && debeCambiarClave; }
    public void setDebeCambiarClave(boolean debeCambiarClave) { this.debeCambiarClave = debeCambiarClave; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
