package com.recovr.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Ficha confidencial de un cliente: preferencias y restricciones que la terapeuta debe respetar. Una por cliente. */
@Entity
@Table(name = "fichas_cliente")
public class FichaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(length = 1000) private String alergias;
    @Column(length = 1000) private String condiciones;
    @Column(name = "presion_preferida", length = 40) private String presionPreferida;
    @Column(length = 255) private String aceites;
    @Column(name = "zonas_restringidas", length = 500) private String zonasRestringidas;
    @Column(length = 2000) private String notas;

    @Column(name = "actualizado_por", length = 120) private String actualizadoPor;
    @Column(name = "actualizado_en") private LocalDateTime actualizadoEn;

    public FichaCliente() {}

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }
    public String getCondiciones() { return condiciones; }
    public void setCondiciones(String condiciones) { this.condiciones = condiciones; }
    public String getPresionPreferida() { return presionPreferida; }
    public void setPresionPreferida(String presionPreferida) { this.presionPreferida = presionPreferida; }
    public String getAceites() { return aceites; }
    public void setAceites(String aceites) { this.aceites = aceites; }
    public String getZonasRestringidas() { return zonasRestringidas; }
    public void setZonasRestringidas(String zonasRestringidas) { this.zonasRestringidas = zonasRestringidas; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(String actualizadoPor) { this.actualizadoPor = actualizadoPor; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
