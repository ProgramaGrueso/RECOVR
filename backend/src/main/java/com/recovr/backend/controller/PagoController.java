package com.recovr.backend.controller;

import com.recovr.backend.entity.Pago;
import com.recovr.backend.service.PagoService;
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
@RequestMapping("/api/pagos")
@Tag(name = "Pagos", description = "Control financiero y transacciones de cobro de sesiones (Requiere ADMIN)")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @Operation(summary = "Listar todas las transacciones de pago", description = "Retorna el historial de pagos efectuados en el centro. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de transacciones de pago obtenido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Pago.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @GetMapping
    public List<Pago> listar() {
        return pagoService.listarTodos();
    }

    @Operation(summary = "Obtener pago por ID", description = "Consulta una transacción de pago específica. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago encontrado",
                    content = @Content(schema = @Schema(implementation = Pago.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public Pago obtener(@Parameter(description = "ID del pago", example = "1") @PathVariable Long id) {
        return pagoService.buscarPorId(id);
    }

    @Operation(summary = "Registrar un pago", description = "Registra manualmente un pago para una reserva. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = Pago.class))),
            @ApiResponse(responseCode = "400", description = "Datos de pago inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping
    public Pago crear(@RequestBody Pago pago) {
        return pagoService.crear(pago);
    }

    @Operation(summary = "Eliminar un registro de pago", description = "Elimina una transacción de pago por su ID. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public void eliminar(@Parameter(description = "ID del pago a eliminar", example = "1") @PathVariable Long id) {
        pagoService.eliminar(id);
    }
}
