package com.recovr.model;

import java.math.BigDecimal;

/**
 * Representa un protocolo o tratamiento ofrecido en RECOVR.
 * Coherente con la tabla 'servicios' del esquema MySQL.
 */
public class Servicio {

    private Long id;
    private String nombre;
    private Integer duracionMinutos;
    private Integer tiempoLimpiezaMinutos;
    private BigDecimal precio;

    public Servicio() {
    }

    public Servicio(Long id, String nombre, Integer duracionMinutos, Integer tiempoLimpiezaMinutos, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
        this.tiempoLimpiezaMinutos = tiempoLimpiezaMinutos;
        this.precio = precio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Integer getTiempoLimpiezaMinutos() {
        return tiempoLimpiezaMinutos;
    }

    public void setTiempoLimpiezaMinutos(Integer tiempoLimpiezaMinutos) {
        this.tiempoLimpiezaMinutos = tiempoLimpiezaMinutos;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
