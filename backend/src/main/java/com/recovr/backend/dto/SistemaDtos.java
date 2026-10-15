package com.recovr.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class SistemaDtos {

    private SistemaDtos() {}

    public record AuditoriaResponse(Long id, LocalDateTime fecha, String usuario, String rol, String accion, String recurso,
                                    Integer estado, String ip, String detalle) {}

    public record EstadoSistemaResponse(
            String version, String java, String springBoot, LocalDateTime iniciadoEn, long segundosActivo,
            boolean baseDatosActiva, String baseDatos,
            long memoriaUsadaMb, long memoriaMaxMb,
            Map<String, Long> registros, Map<String, Long> reservasPorEstado, Map<String, Long> usuariosPorRol,
            LocalDateTime cicloUltimaEjecucion, int cicloUltimosCambios, long cicloTotalCambios,
            long intentosDenegados24h) {}

    public record PermisoResponse(String accion, String metodo, String ruta, List<String> roles, String nota) {}
}
