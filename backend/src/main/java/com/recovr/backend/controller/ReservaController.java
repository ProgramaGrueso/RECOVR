package com.recovr.backend.controller;

import com.recovr.backend.dto.PagoRequest;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.dto.ReservaResponse;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

@RestController("jpaReservaController")
@RequestMapping("/api/db/reservas")
@Tag(name = "Reservas (Persistencia DB)", description = "Gestión de reservas persistidas en base de datos con reglas de rol y usuario autenticado")
public class ReservaController {

    @Autowired
    @Qualifier("jpaReservaService")
    private ReservaService reservaService;

    @Operation(summary = "Listar todas las reservas (DB)", description = "Retorna todas las reservas persistidas. Requiere rol ADMIN o RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de reservas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ADMIN o RECEPCIONISTA)", content = @Content)
    })
    @GetMapping
    public List<ReservaResponse> listar() {
        return aRespuesta(reservaService.listarTodos());
    }

    @Operation(summary = "Obtener reserva por ID (DB)", description = "Consulta una reserva persistida según su ID. Requiere rol ADMIN o RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public ReservaResponse obtener(@Parameter(description = "ID de la reserva", example = "1") @PathVariable Long id) {
        return ReservaResponse.de(reservaService.buscarPorId(id));
    }

    @Operation(
            summary = "Crear reserva",
            description = "Un CLIENTE reserva siempre a su nombre (se ignora clienteId). ADMIN y RECEPCIONISTA deben indicar clienteId. "
                    + "Se valida que la sala y el especialista no tengan otra reserva activa que se cruce, considerando duración y tiempo de limpieza."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada en estado PENDIENTE",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o fecha en el pasado", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente, especialista, servicio o sala inexistente", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto por solapamiento de horario", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request, Authentication authentication) {
        return ReservaResponse.de(reservaService.crearDesdeRequest(
                request, authentication.getName(), esGestionDeReservas(authentication)));
    }

    @Operation(summary = "Actualizar reserva", description = "Modifica una reserva existente (incluido su estado). Requiere rol ADMIN o RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva actualizada",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto por solapamiento de horario", content = @Content)
    })
    @PutMapping("/{id}")
    public ReservaResponse actualizar(
            @Parameter(description = "ID de la reserva", example = "1") @PathVariable Long id,
            @Valid @RequestBody ReservaRequest request) {
        return ReservaResponse.de(reservaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar reserva", description = "Elimina una reserva de la base de datos por su ID. Requiere rol ADMIN o RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Parameter(description = "ID de la reserva", example = "1") @PathVariable Long id) {
        reservaService.eliminar(id);
    }

    @Operation(
            summary = "Buscar reservas por ID de cliente",
            description = "Retorna reservas de un cliente. Los clientes solo pueden ver las suyas; ADMIN y RECEPCIONISTA pueden consultar cualquier cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas del cliente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso no autorizado al historial de otro cliente", content = @Content)
    })
    @GetMapping("/cliente/{clienteId}")
    public List<ReservaResponse> porCliente(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long clienteId,
            Authentication authentication) {
        return aRespuesta(reservaService.buscarPorClienteParaUsuario(clienteId, authentication.getName(), esGestionDeReservas(authentication)));
    }

    @Operation(summary = "Obtener mis reservas (Cliente autenticado)", description = "Devuelve el historial de reservas pertenecientes al CLIENTE en sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas propias",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Solo para rol CLIENTE", content = @Content)
    })
    @GetMapping("/mias")
    public List<ReservaResponse> misReservas(Authentication authentication) {
        return aRespuesta(reservaService.buscarMisReservas(authentication.getName()));
    }

    @Operation(summary = "Agenda del especialista autenticado", description = "Devuelve las reservas futuras del ESPECIALISTA en sesión, ordenadas por fecha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda del especialista",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResponse.class)))),
            @ApiResponse(responseCode = "400", description = "El usuario no está vinculado a un especialista", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Solo para rol ESPECIALISTA", content = @Content)
    })
    @GetMapping("/agenda")
    public List<ReservaResponse> agenda(Authentication authentication) {
        return aRespuesta(reservaService.buscarAgendaEspecialista(authentication.getName()));
    }

    @Operation(summary = "Buscar reservas por rango de fecha y estado", description = "Filtra reservas entre fechas de inicio y fin según su estado. Requiere rol ADMIN o RECEPCIONISTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas filtradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Rango de fechas inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content)
    })
    @GetMapping("/buscar")
    public List<ReservaResponse> porRangoYEstado(
            @Parameter(description = "Fecha/hora inicio (ISO-8601)", example = "2026-10-01T08:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fecha/hora fin (ISO-8601)", example = "2026-10-31T20:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @Parameter(description = "Estado de la reserva", example = "CONFIRMADA")
            @RequestParam EstadoReserva estado) {
        return aRespuesta(reservaService.buscarPorRangoYEstado(inicio, fin, estado));
    }

    @Operation(
            summary = "Confirmar y registrar pago de una reserva",
            description = "Transaccionalmente confirma una reserva PENDIENTE y registra el pago correspondiente. Un CLIENTE solo puede pagar las suyas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada y pago registrado",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de pago inválidos o reserva no PENDIENTE", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @PostMapping("/{id}/confirmar-y-pagar")
    public ReservaResponse confirmarYPagar(
            @Parameter(description = "ID de la reserva a confirmar y pagar", example = "1") @PathVariable Long id,
            @Valid @RequestBody PagoRequest pago,
            Authentication authentication) {
        return ReservaResponse.de(reservaService.confirmarYPagarParaUsuario(
                id, pago, authentication.getName(), esGestionDeReservas(authentication)));
    }

    private List<ReservaResponse> aRespuesta(List<Reserva> reservas) {
        return reservas.stream().map(ReservaResponse::de).toList();
    }

    private boolean esGestionDeReservas(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_RECEPCIONISTA"));
    }
}
