package com.recovr.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Contratos de la ficha confidencial del cliente y de las notas de sesión. */
public final class FichaDtos {

    private FichaDtos() {}

    public record FichaRequest(
            @Size(max = 1000) String alergias,
            @Size(max = 1000) String condiciones,
            @Size(max = 40) String presionPreferida,
            @Size(max = 255) String aceites,
            @Size(max = 500) String zonasRestringidas,
            @Size(max = 2000) String notas) {}

    public record NotaRequest(
            @NotBlank @Size(max = 200) String tratamiento,
            @Size(max = 2000) String observaciones,
            @Size(max = 2000) String recomendaciones,
            Long reservaId) {}

    public record NotaResponse(Long id, LocalDateTime fecha, String terapeuta, String tratamiento, String observaciones,
                               String recomendaciones, Long reservaId, boolean propia) {}

    public record FichaResponse(Long clienteId, String clienteNombre, String telefono, String alergias, String condiciones,
                                String presionPreferida, String aceites, String zonasRestringidas, String notas,
                                String actualizadoPor, LocalDateTime actualizadoEn, List<NotaResponse> notasSesion) {}

    public record MiClienteResponse(Long clienteId, String nombre, String telefono, long sesiones,
                                    LocalDateTime ultimaSesion, LocalDateTime proximaSesion, boolean tieneFicha) {}
}
