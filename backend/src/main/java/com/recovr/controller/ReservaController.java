package com.recovr.controller;

import com.recovr.dto.CrearReservaRequest;
import com.recovr.dto.ReservaResponse;
import com.recovr.model.Reserva;
import com.recovr.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST delgado para la gestión de reservas en RECOVR.
 * Su responsabilidad se limita a recibir peticiones HTTP, mapear datos y delegar al Core Service.
 */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas (Core Engine)", description = "Controlador de reservas con DTOs desacoplados y contratos semánticos RESTful (201, 200, 204, 400, 404, 409)")
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Inyección de Dependencias por Constructor:
     * Spring inyecta la instancia gestionada del componente ReservaService.
     */
    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * Endpoint para registrar una nueva reserva.
     * Retorna HTTP 201 Created junto con la representación en DTO.
     */
    @Operation(
            summary = "Registrar nueva reserva",
            description = "Crea una nueva reserva en estado PENDIENTE evaluando la disponibilidad de especialista y cabina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o fecha en el pasado", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto de agenda por solapamiento de turno o sala ocupada", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ReservaResponse> crearReserva(@RequestBody CrearReservaRequest request) {
        Reserva creada = reservaService.crearReserva(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.fromDomain(creada));
    }

    /**
     * Endpoint para listar todas las reservas registradas.
     * Retorna HTTP 200 OK con la colección de recursos.
     */
    @Operation(summary = "Listar todas las reservas", description = "Retorna la colección completa de reservas registradas en el Core.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ADMIN o RECEPCIONISTA)", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listarTodas() {
        List<ReservaResponse> lista = reservaService.listarTodas().stream()
                .map(ReservaResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(lista);
    }

    /**
     * Endpoint para consultar una reserva por su ID.
     */
    @Operation(summary = "Consultar reserva por ID", description = "Retorna el detalle de una reserva específica a partir de su ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ADMIN o RECEPCIONISTA)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtenerPorId(
            @Parameter(description = "Identificador de la reserva", example = "1") @PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id);
        return ResponseEntity.ok(ReservaResponse.fromDomain(reserva));
    }

    /**
     * Endpoint para confirmar una reserva existente.
     */
    @Operation(summary = "Confirmar reserva", description = "Transiciona el estado de la reserva de PENDIENTE a CONFIRMADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Operación no permitida (reserva previamente cancelada)", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<ReservaResponse> confirmarReserva(
            @Parameter(description = "Identificador de la reserva a confirmar", example = "1") @PathVariable Long id) {
        Reserva confirmada = reservaService.confirmarReserva(id);
        return ResponseEntity.ok(ReservaResponse.fromDomain(confirmada));
    }

    /**
     * Endpoint para cancelar una reserva.
     */
    @Operation(summary = "Cancelar reserva", description = "Transiciona el estado de la reserva a CANCELADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva cancelada",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Operación no permitida (reserva ya completada)", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelarReserva(
            @Parameter(description = "Identificador de la reserva a cancelar", example = "1") @PathVariable Long id) {
        Reserva cancelada = reservaService.cancelarReserva(id);
        return ResponseEntity.ok(ReservaResponse.fromDomain(cancelada));
    }

    /**
     * Endpoint para anular / eliminar una reserva (Baja lógica idempotente).
     * Aplica la semántica HTTP DELETE retornando HTTP 204 No Content.
     */
    @Operation(summary = "Eliminar / anular reserva", description = "Cancela la reserva aplicando la semántica HTTP DELETE (204 No Content).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva anulada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Operación no permitida (reserva ya completada)", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarReserva(
            @Parameter(description = "Identificador de la reserva", example = "1") @PathVariable Long id) {
        reservaService.cancelarReserva(id);
        return ResponseEntity.noContent().build();
    }
}
