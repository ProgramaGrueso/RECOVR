package com.recovr.backend.controller;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.service.EmpleadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/empleados")
@Tag(name = "Empleados", description = "Gestión del equipo de especialistas y terapeutas de recuperación (Requiere ADMIN)")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @Operation(summary = "Listar todos los especialistas", description = "Retorna el listado completo de terapeutas/empleados. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empleados recuperada",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Empleado.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping
    public List<Empleado> listar() {
        return empleadoService.listarTodos();
    }

    @Operation(summary = "Obtener especialista por ID", description = "Obtiene los detalles del especialista según su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado encontrado",
                    content = @Content(schema = @Schema(implementation = Empleado.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public Empleado obtener(@Parameter(description = "ID del empleado", example = "1") @PathVariable Long id) {
        return empleadoService.buscarPorId(id);
    }

    @Operation(summary = "Registrar nuevo especialista", description = "Registra un nuevo especialista en la plataforma. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado creado exitosamente",
                    content = @Content(schema = @Schema(implementation = Empleado.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping
    public Empleado crear(@RequestBody Empleado empleado) {
        return empleadoService.crear(empleado);
    }

    @Operation(summary = "Actualizar datos del especialista", description = "Actualiza información como nombre, especialidad o teléfono. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado actualizado",
                    content = @Content(schema = @Schema(implementation = Empleado.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public Empleado actualizar(
            @Parameter(description = "ID del empleado a actualizar", example = "1") @PathVariable Long id,
            @RequestBody Empleado empleado) {
        return empleadoService.actualizar(id, empleado);
    }

    @Operation(summary = "Eliminar especialista", description = "Elimina un empleado por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public void eliminar(@Parameter(description = "ID del empleado a eliminar", example = "1") @PathVariable Long id) {
        empleadoService.eliminar(id);
    }

    @Operation(
            summary = "Consultar especialistas disponibles en un rango de tiempo",
            description = "Filtra terapeutas que no tienen reservas asignadas durante la ventana horaria especificada. Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empleados disponibles",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Empleado.class)))),
            @ApiResponse(responseCode = "400", description = "Parámetros de fecha/hora incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping("/disponibles")
    public List<Empleado> disponibles(
            @Parameter(description = "Fecha/hora de inicio (ISO-8601)", example = "2026-10-15T09:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fecha/hora de fin (ISO-8601)", example = "2026-10-15T11:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return empleadoService.buscarDisponibles(inicio, fin);
    }
}
