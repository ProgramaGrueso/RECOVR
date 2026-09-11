package com.recovr.backend.controller;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
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

    @Operation(summary = "Listar todas las reservas (DB)", description = "Retorna todas las reservas persistidas en el sistema. Requiere autenticación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de reservas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Reserva.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    @GetMapping
    public List<Reserva> listar() {
        return reservaService.listarTodos();
    }

    @Operation(summary = "Obtener reserva por ID (DB)", description = "Consulta una reserva persistida según su ID. Requiere autenticación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada",
                    content = @Content(schema = @Schema(implementation = Reserva.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public Reserva obtener(@Parameter(description = "ID de la reserva", example = "1") @PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    @Operation(
            summary = "Crear reserva para el usuario autenticado",
            description = "Crea una reserva vinculando al cliente autenticado o permitiendo asignación a cualquier cliente si es ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva creada exitosamente",
                    content = @Content(schema = @Schema(implementation = Reserva.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o inconsistentes", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflicto por solapamiento de horario", content = @Content)
    })
    @PostMapping
    public Reserva crear(@RequestBody Reserva reserva, Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return reservaService.crearParaUsuarioAutenticado(reserva, authentication.getName(), esAdmin);
    }

    @Operation(summary = "Actualizar reserva", description = "Modifica una reserva existente en base de datos. Requiere autenticación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva actualizada",
                    content = @Content(schema = @Schema(implementation = Reserva.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @PutMapping("/{id}")
    public Reserva actualizar(
            @Parameter(description = "ID de la reserva", example = "1") @PathVariable Long id,
            @RequestBody Reserva reserva) {
        return reservaService.actualizar(id, reserva);
    }

    @Operation(summary = "Eliminar reserva", description = "Elimina una reserva de la base de datos por su ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva eliminada"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    public void eliminar(@Parameter(description = "ID de la reserva", example = "1") @PathVariable Long id) {
        reservaService.eliminar(id);
    }

    @Operation(
            summary = "Buscar reservas por ID de cliente",
            description = "Retorna reservas de un cliente. Los clientes solo pueden ver las suyas; ADMIN y RECEPCIONISTA pueden consultar cualquier cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas del cliente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Reserva.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso no autorizado al historial de otro cliente", content = @Content)
    })
    @GetMapping("/cliente/{clienteId}")
    public List<Reserva> porCliente(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long clienteId,
            Authentication authentication) {
        return reservaService.buscarPorClienteParaUsuario(clienteId, authentication.getName(), esGestionDeReservas(authentication));
    }

    @Operation(summary = "Obtener mis reservas (Cliente autenticado)", description = "Devuelve el historial de reservas pertenecientes al CLIENTE en sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas propias",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Reserva.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    @GetMapping("/mias")
    public List<Reserva> misReservas(Authentication authentication) {
        return reservaService.buscarMisReservas(authentication.getName());
    }

    @Operation(summary = "Buscar reservas por rango de fecha y estado", description = "Filtra reservas entre fechas de inicio y fin según su estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas filtradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Reserva.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    @GetMapping("/buscar")
    public List<Reserva> porRangoYEstado(
            @Parameter(description = "Fecha/hora inicio (ISO-8601)", example = "2026-10-01T08:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fecha/hora fin (ISO-8601)", example = "2026-10-31T20:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @Parameter(description = "Estado de la reserva", example = "CONFIRMADA")
            @RequestParam EstadoReserva estado) {
        return reservaService.buscarPorRangoYEstado(inicio, fin, estado);
    }

    @Operation(
            summary = "Confirmar y registrar pago de una reserva",
            description = "Transaccionalmente confirma la reserva y emite el pago correspondiente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada y pago registrado",
                    content = @Content(schema = @Schema(implementation = Reserva.class))),
            @ApiResponse(responseCode = "400", description = "Datos de pago inválidos o estado inconsistente", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada", content = @Content)
    })
    @PostMapping("/{id}/confirmar-y-pagar")
    public Reserva confirmarYPagar(
            @Parameter(description = "ID de la reserva a confirmar y pagar", example = "1") @PathVariable Long id,
            @RequestBody Pago pago,
            Authentication authentication) {
        return reservaService.confirmarYPagarParaUsuario(id, pago, authentication.getName(), esGestionDeReservas(authentication));
    }

    private boolean esGestionDeReservas(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_RECEPCIONISTA"));
    }
}
