package com.recovr.backend.controller;

import com.recovr.backend.dto.PuntosDtos.CanjeValidoResponse;
import com.recovr.backend.service.PuntosService;
import com.recovr.backend.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/canjes")
@Tag(name = "Canjes (recepción)", description = "Validación de códigos de canje antes de cobrar. ADMIN o RECEPCIONISTA.")
public class CanjeController {

    private final PuntosService puntosService;
    private final ReservaService reservaService;

    public CanjeController(PuntosService puntosService, ReservaService reservaService) {
        this.puntosService = puntosService;
        this.reservaService = reservaService;
    }

    @Operation(summary = "Validar un código de canje para una reserva",
            description = "Indica si el código sirve para esa reserva (mismo cliente, sin usar) y qué monto corresponde cobrar.")
    @GetMapping("/{codigo}")
    public CanjeValidoResponse validar(@PathVariable String codigo, @RequestParam Long reservaId) {
        return puntosService.vistaPrevia(codigo, reservaService.buscarPorId(reservaId));
    }
}
