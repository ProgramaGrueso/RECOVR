package com.recovr.backend.controller;

import com.recovr.backend.dto.EmpleadoEstadoResponse;
import com.recovr.backend.dto.ResumenResponse;
import com.recovr.backend.dto.SalaEstadoResponse;
import com.recovr.backend.service.OperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/operacion")
@Tag(name = "Operación en vivo", description = "Estado de cabinas y terapeutas e indicadores del panel. Requiere ADMIN o RECEPCIONISTA.")
public class OperacionController {

    private final OperacionService operacionService;

    public OperacionController(OperacionService operacionService) {
        this.operacionService = operacionService;
    }

    @Operation(summary = "Estado en vivo de las cabinas",
            description = "DISPONIBLE, OCUPADA, ESPERANDO_CLIENTE o EN_SANITIZACION, deducido de las reservas y la hora actual.")
    @GetMapping("/salas")
    public List<SalaEstadoResponse> salas() {
        return operacionService.estadoSalas(LocalDateTime.now());
    }

    @Operation(summary = "Disponibilidad en vivo de las terapeutas", description = "LIBRE, EN_SESION o POR_INICIAR.")
    @GetMapping("/empleados")
    public List<EmpleadoEstadoResponse> empleados() {
        return operacionService.estadoEmpleados(LocalDateTime.now());
    }

    @Operation(summary = "Indicadores del panel",
            description = "Ingresos del mes, ocupación de hoy, sesiones completadas y tasa de cancelación, calculados desde la base.")
    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return operacionService.resumen(LocalDateTime.now());
    }
}
