package com.recovr.backend.controller;

import com.recovr.backend.dto.SistemaDtos.*;
import com.recovr.backend.security.MatrizPermisos;
import com.recovr.backend.service.SistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Sistema y seguridad", description = "Estado del sistema, auditoría y matriz de permisos. Solo ADMIN.")
public class SistemaController {

    private final SistemaService sistemaService;

    public SistemaController(SistemaService sistemaService) {
        this.sistemaService = sistemaService;
    }

    @Operation(summary = "Estado del sistema", description = "Base de datos, memoria, volúmenes de datos, ciclo de vida de reservas e intentos denegados.")
    @GetMapping("/api/sistema/estado")
    public EstadoSistemaResponse estado() {
        return sistemaService.estado();
    }

    @Operation(summary = "Auditoría", description = "Últimos eventos: inicios de sesión, operaciones que modifican datos y accesos denegados.")
    @GetMapping("/api/sistema/auditoria")
    public List<AuditoriaResponse> auditoria(@RequestParam(defaultValue = "100") int limite) {
        return sistemaService.auditoria(limite);
    }

    @Operation(summary = "Matriz de permisos por rol",
            description = "Qué rol puede hacer qué. Una prueba automática verifica cada fila contra el comportamiento real de la API.")
    @GetMapping("/api/seguridad/matriz")
    public List<PermisoResponse> matriz() {
        return MatrizPermisos.todos().stream()
                .map(p -> new PermisoResponse(p.accion(), p.metodo(), p.ruta(), p.roles().stream().map(Enum::name).sorted().toList(), p.nota()))
                .toList();
    }
}
