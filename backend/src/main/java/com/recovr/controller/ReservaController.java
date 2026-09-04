package com.recovr.controller;

import com.recovr.dto.CrearReservaRequest;
import com.recovr.dto.ReservaResponse;
import com.recovr.model.Reserva;
import com.recovr.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST delgado para la gestión de reservas en RECOVR.
 * Su responsabilidad se limita a recibir peticiones HTTP, mapear datos y delegar al Core Service.
 */
@RestController
@RequestMapping("/api/reservas")
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
    @PostMapping
    public ResponseEntity<ReservaResponse> crearReserva(@RequestBody CrearReservaRequest request) {
        Reserva reserva = new Reserva(
                null,
                request.getClienteId(),
                request.getEmpleadoId(),
                request.getServicioId(),
                request.getSalaId(),
                request.getFechaHora(),
                request.getDuracionTotalMinutos(),
                null,
                request.getMontoTotal()
        );

        Reserva creada = reservaService.crearReserva(reserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.fromDomain(creada));
    }

    /**
     * Endpoint para consultar una reserva por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtenerPorId(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id);
        return ResponseEntity.ok(ReservaResponse.fromDomain(reserva));
    }

    /**
     * Endpoint para confirmar una reserva existente.
     */
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<ReservaResponse> confirmarReserva(@PathVariable Long id) {
        Reserva confirmada = reservaService.confirmarReserva(id);
        return ResponseEntity.ok(ReservaResponse.fromDomain(confirmada));
    }

    /**
     * Endpoint para cancelar una reserva.
     */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelarReserva(@PathVariable Long id) {
        Reserva cancelada = reservaService.cancelarReserva(id);
        return ResponseEntity.ok(ReservaResponse.fromDomain(cancelada));
    }
}
