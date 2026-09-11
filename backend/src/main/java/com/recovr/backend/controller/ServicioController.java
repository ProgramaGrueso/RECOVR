package com.recovr.backend.controller;

import com.recovr.backend.entity.Servicio;
import com.recovr.backend.service.ServicioService;
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
@RequestMapping("/api/servicios")
@Tag(name = "Servicios", description = "Catálogo de servicios y terapias de recuperación (Lectura para usuarios autenticados; mutaciones para ADMIN)")
public class ServicioController {

    @Autowired
    private ServicioService servicioService;

    @Operation(
            summary = "Listar catálogo de servicios",
            description = "Retorna el catálogo completo de terapias disponibles en RECOVR. Accesible para ADMIN, RECEPCIONISTA, ESPECIALISTA y CLIENTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo de servicios recuperado",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Servicio.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content)
    })
    @GetMapping
    public List<Servicio> listar() {
        return servicioService.listarTodos();
    }

    @Operation(
            summary = "Obtener servicio por ID",
            description = "Consulta la información de un servicio específico. Accesible para ADMIN, RECEPCIONISTA, ESPECIALISTA y CLIENTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio encontrado",
                    content = @Content(schema = @Schema(implementation = Servicio.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public Servicio obtener(@Parameter(description = "ID del servicio", example = "1") @PathVariable Long id) {
        return servicioService.buscarPorId(id);
    }

    @Operation(summary = "Crear nuevo servicio", description = "Agrega una nueva terapia o tratamiento al catálogo. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio creado exitosamente",
                    content = @Content(schema = @Schema(implementation = Servicio.class))),
            @ApiResponse(responseCode = "400", description = "Datos de servicio inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping
    public Servicio crear(@RequestBody Servicio servicio) {
        return servicioService.crear(servicio);
    }

    @Operation(summary = "Actualizar servicio existente", description = "Modifica duración, tiempo de limpieza, precio o nombre. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = Servicio.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public Servicio actualizar(
            @Parameter(description = "ID del servicio a actualizar", example = "1") @PathVariable Long id,
            @RequestBody Servicio servicio) {
        return servicioService.actualizar(id, servicio);
    }

    @Operation(summary = "Eliminar servicio", description = "Elimina un servicio del catálogo. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public void eliminar(@Parameter(description = "ID del servicio a eliminar", example = "1") @PathVariable Long id) {
        servicioService.eliminar(id);
    }
}
