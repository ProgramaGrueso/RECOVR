package com.recovr.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Canje de puntos de un cliente por un beneficio. Queda DISPONIBLE con un código único; recepción lo aplica al cobrar
 * una sesión del mismo cliente y entonces pasa a USADO (un código sirve una sola vez).
 */
@Entity
@Table(name = "canjes")
public class Canje {

    public enum Estado { DISPONIBLE, USADO, ANULADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "beneficio_id", nullable = false, length = 40) private String beneficioId;
    @Column(nullable = false, length = 150) private String titulo;
    @Column(nullable = false) private int puntos;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal descuento;
    @Column(nullable = false, unique = true, length = 20) private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.DISPONIBLE;

    @Column(name = "creado_en", nullable = false) private LocalDateTime creadoEn;
    @Column(name = "usado_en") private LocalDateTime usadoEn;
    /** Hasta cuándo sirve el código (nulo en los emitidos antes de existir la vigencia: no vencen). */
    @Column(name = "vence_en") private LocalDateTime venceEn;

    @ManyToOne
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    public Canje() {}

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getBeneficioId() { return beneficioId; }
    public void setBeneficioId(String beneficioId) { this.beneficioId = beneficioId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public LocalDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(LocalDateTime venceEn) { this.venceEn = venceEn; }
    public boolean vencido(LocalDateTime ahora) { return estado == Estado.DISPONIBLE && venceEn != null && !ahora.isBefore(venceEn); }
    public LocalDateTime getUsadoEn() { return usadoEn; }
    public void setUsadoEn(LocalDateTime usadoEn) { this.usadoEn = usadoEn; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
}
