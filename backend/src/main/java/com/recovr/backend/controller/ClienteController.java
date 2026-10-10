package com.recovr.backend.controller;

import com.recovr.backend.dto.ClienteResponse;
import com.recovr.backend.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.recovr.backend.dto.ClienteRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Gestión integral de clientes (CRUD exclusivo para ADMIN)")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(summary = "Listar todos los clientes", description = "Obtiene la lista completa de clientes registrados en el sistema. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listarTodos().stream().map(ClienteResponse::de).toList();
    }

    @Operation(summary = "Obtener cliente por ID", description = "Consulta la información detallada de un cliente específico. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ClienteResponse obtener(@Parameter(description = "ID único del cliente", example = "1") @PathVariable Long id) {
        return ClienteResponse.de(clienteService.buscarPorId(id));
    }

    @Operation(summary = "Crear nuevo cliente", description = "Registra manualmente un cliente en la base de datos. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de cliente inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.de(clienteService.crear(request.aEntidad()));
    }

    @Operation(summary = "Actualizar cliente", description = "Modifica los datos de un cliente existente identificado por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ClienteResponse actualizar(
            @Parameter(description = "ID único del cliente a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.de(clienteService.actualizar(id, request.aEntidad()));
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente de la base de datos por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Parameter(description = "ID único del cliente a eliminar", example = "1") @PathVariable Long id) {
        clienteService.eliminar(id);
    }
}
