package com.recovr.backend.controller;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Gestión integral de clientes (CRUD exclusivo para ADMIN)")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Operation(summary = "Listar todos los clientes", description = "Obtiene la lista completa de clientes registrados en el sistema. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Cliente.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listarTodos();
    }

    @Operation(summary = "Obtener cliente por ID", description = "Consulta la información detallada de un cliente específico. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = Cliente.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public Cliente obtener(@Parameter(description = "ID único del cliente", example = "1") @PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @Operation(summary = "Crear nuevo cliente", description = "Registra manualmente un cliente en la base de datos. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente creado exitosamente",
                    content = @Content(schema = @Schema(implementation = Cliente.class))),
            @ApiResponse(responseCode = "400", description = "Datos de cliente inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping
    public Cliente crear(@RequestBody Cliente cliente) {
        return clienteService.crear(cliente);
    }

    @Operation(summary = "Actualizar cliente", description = "Modifica los datos de un cliente existente identificado por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = Cliente.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public Cliente actualizar(
            @Parameter(description = "ID único del cliente a actualizar", example = "1") @PathVariable Long id,
            @RequestBody Cliente cliente) {
        return clienteService.actualizar(id, cliente);
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente de la base de datos por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public void eliminar(@Parameter(description = "ID único del cliente a eliminar", example = "1") @PathVariable Long id) {
        clienteService.eliminar(id);
    }
}
