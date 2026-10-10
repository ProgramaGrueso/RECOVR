package com.recovr.backend.controller;

import com.recovr.backend.dto.SalaResponse;
import com.recovr.backend.service.SalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import com.recovr.backend.dto.SalaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/salas")
@Tag(name = "Salas", description = "Gestión de suites y salas de tratamiento de RECOVR (Requiere ADMIN)")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @Operation(summary = "Listar todas las salas", description = "Retorna el listado de salas y cabinas terapéuticas. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de salas obtenido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SalaResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping
    public List<SalaResponse> listar() {
        return salaService.listarTodos().stream().map(SalaResponse::de).toList();
    }

    @Operation(summary = "Obtener sala por ID", description = "Obtiene los datos de una sala por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala encontrada",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public SalaResponse obtener(@Parameter(description = "ID de la sala", example = "1") @PathVariable Long id) {
        return SalaResponse.de(salaService.buscarPorId(id));
    }

    @Operation(summary = "Crear nueva sala", description = "Registra una nueva sala o suite terapéutica. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sala creada",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaResponse crear(@Valid @RequestBody SalaRequest request) {
        return SalaResponse.de(salaService.crear(request.aEntidad()));
    }

    @Operation(summary = "Actualizar sala", description = "Actualiza nombre o capacidad de una sala. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala actualizada exitosamente",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada", content = @Content)
    })
    @PutMapping("/{id}")
    public SalaResponse actualizar(
            @Parameter(description = "ID de la sala a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody SalaRequest request) {
        return SalaResponse.de(salaService.actualizar(id, request.aEntidad()));
    }

    @Operation(summary = "Eliminar sala", description = "Elimina una sala del sistema. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sala eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Parameter(description = "ID de la sala a eliminar", example = "1") @PathVariable Long id) {
        salaService.eliminar(id);
    }

    @Operation(
            summary = "Consultar salas disponibles",
            description = "Filtra salas libres de reservas dentro de la ventana horaria especificada. Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de salas disponibles",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SalaResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Parámetros de fecha/hora incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping("/disponibles")
    public List<SalaResponse> disponibles(
            @Parameter(description = "Fecha/hora de inicio (ISO-8601)", example = "2026-10-15T09:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fecha/hora de fin (ISO-8601)", example = "2026-10-15T11:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return salaService.buscarDisponibles(inicio, fin).stream().map(SalaResponse::de).toList();
    }
}
