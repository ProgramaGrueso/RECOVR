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
    private final com.recovr.backend.service.PuntosService puntosService;

    public MiCuentaController(MiCuentaService miCuentaService, com.recovr.backend.service.PuntosService puntosService) {
        this.miCuentaService = miCuentaService;
        this.puntosService = puntosService;
    }

    @Operation(summary = "Resumen de mi cuenta", description = "Perfil, puntos y nivel calculados de mis sesiones completadas, próxima reserva y cancelaciones del mes.")
    @GetMapping("/resumen")
    public MiCuentaResponse resumen(Authentication auth) {
        return miCuentaService.resumen(auth.getName());
    }

    @Operation(summary = "Mis puntos", description = "Reglas, saldo, nivel, beneficios canjeables, historial de movimientos y códigos de canje.")
    @GetMapping("/puntos")
    public com.recovr.backend.dto.PuntosDtos.PuntosResponse puntos(Authentication auth) {
        return puntosService.detalle(auth.getName());
    }

    @Operation(summary = "Anular un código de canje sin usar", description = "Devuelve los puntos del código. No aplica a códigos usados, vencidos ni a créditos por cancelación.")
    @PostMapping("/canjes/{codigo}/anular")
    public com.recovr.backend.dto.PuntosDtos.CanjeInfo anular(@PathVariable String codigo, Authentication auth) {
        return puntosService.anular(auth.getName(), codigo);
    }

    @Operation(summary = "Canjear puntos por un beneficio", description = "Genera un código de un solo uso que recepción aplica al cobrar.")
    @PostMapping("/canjes")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public com.recovr.backend.dto.PuntosDtos.CanjeInfo canjear(@Valid @RequestBody com.recovr.backend.dto.PuntosDtos.CanjearRequest request, Authentication auth) {
        return puntosService.canjear(auth.getName(), request.beneficioId());
    }

    @Operation(summary = "Actualizar mi nombre y teléfono")
    @PutMapping
    public MiCuentaResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest request, Authentication auth) {
        return miCuentaService.actualizar(auth.getName(), request);
    }
}
