package com.recovr.backend.controller;

import com.recovr.backend.dto.EmpleadoCatalogoResponse;
import com.recovr.backend.dto.EmpleadoResponse;
import com.recovr.backend.service.EmpleadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import com.recovr.backend.dto.EmpleadoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/empleados")
@Tag(name = "Empleados", description = "Gestión del equipo de especialistas y terapeutas de recuperación (Requiere ADMIN)")
public class EmpleadoController {

    private final EmpleadoService empleadoService;
    private final com.recovr.backend.service.FotoService fotoService;

    public EmpleadoController(EmpleadoService empleadoService, com.recovr.backend.service.FotoService fotoService) {
        this.empleadoService = empleadoService;
        this.fotoService = fotoService;
    }

    @Operation(summary = "Listar todos los especialistas", description = "Retorna el listado completo de terapeutas/empleados. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empleados recuperada",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EmpleadoResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping
    public List<EmpleadoResponse> listar() {
        return empleadoService.listarTodos().stream().map(EmpleadoResponse::de).toList();
    }

    @Operation(summary = "Catálogo de especialistas", description = "Lista id, nombre y especialidad para armar una reserva. Accesible para cualquier usuario autenticado.")
    @GetMapping("/catalogo")
    public List<EmpleadoCatalogoResponse> catalogo() {
        return empleadoService.listarActivos().stream().map(EmpleadoCatalogoResponse::de).toList();
    }

    @Operation(summary = "Subir la foto de una terapeuta", description = "JPG o PNG de hasta 3 MB. Solo ADMIN (la terapeuta puede subir la suya en /api/mi-perfil/foto).")
    @PutMapping(path = "/{id}/foto", consumes = "multipart/form-data")
    public EmpleadoResponse subirFoto(@PathVariable Long id, @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo)
            throws java.io.IOException {
        fotoService.guardar(id, archivo.getBytes());
        return EmpleadoResponse.de(empleadoService.buscarPorId(id));
    }

    @Operation(summary = "Quitar la foto de una terapeuta")
    @DeleteMapping("/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarFoto(@PathVariable Long id) {
        empleadoService.buscarPorId(id);
        fotoService.eliminar(id);
    }

    @Operation(summary = "Dar de baja o reactivar a un especialista",
            description = "Baja lógica: conserva el historial pero deja de recibir reservas. Falla si tiene reservas activas. Requiere rol ADMIN.")
    @PatchMapping("/{id}/activo")
    public EmpleadoResponse cambiarActivo(@PathVariable Long id, @RequestParam boolean valor) {
        return EmpleadoResponse.de(empleadoService.cambiarActivo(id, valor));
    }

    @Operation(summary = "Obtener especialista por ID", description = "Obtiene los detalles del especialista según su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado encontrado",
                    content = @Content(schema = @Schema(implementation = EmpleadoResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public EmpleadoResponse obtener(@Parameter(description = "ID del empleado", example = "1") @PathVariable Long id) {
        return EmpleadoResponse.de(empleadoService.buscarPorId(id));
    }

    @Operation(summary = "Registrar nuevo especialista", description = "Registra un nuevo especialista en la plataforma. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empleado creado exitosamente",
                    content = @Content(schema = @Schema(implementation = EmpleadoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpleadoResponse crear(@Valid @RequestBody EmpleadoRequest request) {
        return EmpleadoResponse.de(empleadoService.crear(request.aEntidad()));
    }

    @Operation(summary = "Actualizar datos del especialista", description = "Actualiza información como nombre, especialidad o teléfono. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado actualizado",
                    content = @Content(schema = @Schema(implementation = EmpleadoResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public EmpleadoResponse actualizar(
            @Parameter(description = "ID del empleado a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody EmpleadoRequest request) {
        return EmpleadoResponse.de(empleadoService.actualizar(id, request.aEntidad()));
    }

    @Operation(summary = "Eliminar especialista", description = "Elimina un empleado por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empleado eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Parameter(description = "ID del empleado a eliminar", example = "1") @PathVariable Long id) {
        empleadoService.eliminar(id);
    }

    @Operation(
            summary = "Consultar especialistas disponibles en un rango de tiempo",
            description = "Filtra terapeutas que no tienen reservas asignadas durante la ventana horaria especificada. Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empleados disponibles",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EmpleadoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Parámetros de fecha/hora incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping("/disponibles")
    public List<EmpleadoResponse> disponibles(
            @Parameter(description = "Fecha/hora de inicio (ISO-8601)", example = "2026-10-15T09:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(description = "Fecha/hora de fin (ISO-8601)", example = "2026-10-15T11:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return empleadoService.buscarDisponibles(inicio, fin).stream().map(EmpleadoResponse::de).toList();
    }
}
