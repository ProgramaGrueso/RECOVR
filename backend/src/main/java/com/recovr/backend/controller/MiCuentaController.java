package com.recovr.backend.controller;

import com.recovr.backend.dto.MiCuentaDtos.*;
import com.recovr.backend.service.MiCuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mi-cuenta")
@Tag(name = "Mi cuenta (cliente)", description = "Portal del cliente autenticado: solo ve y modifica lo suyo.")
public class MiCuentaController {

    private final MiCuentaService miCuentaService;

    public MiCuentaController(MiCuentaService miCuentaService) {
        this.miCuentaService = miCuentaService;
    }

    @Operation(summary = "Resumen de mi cuenta", description = "Perfil, puntos y nivel calculados de mis sesiones completadas, próxima reserva y cancelaciones del mes.")
    @GetMapping("/resumen")
    public MiCuentaResponse resumen(Authentication auth) {
        return miCuentaService.resumen(auth.getName());
    }

    @Operation(summary = "Actualizar mi nombre y teléfono")
    @PutMapping
    public MiCuentaResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest request, Authentication auth) {
        return miCuentaService.actualizar(auth.getName(), request);
    }
}
