package com.recovr.backend.controller;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @GetMapping
    public List<Reserva> listar() {
        return reservaService.listarTodos();
    }

    @GetMapping("/{id}")
    public Reserva obtener(@PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    @PostMapping
    public Reserva crear(@RequestBody Reserva reserva) {
        return reservaService.crear(reserva);
    }

    @PutMapping("/{id}")
    public Reserva actualizar(@PathVariable Long id, @RequestBody Reserva reserva) {
        return reservaService.actualizar(id, reserva);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        reservaService.eliminar(id);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<Reserva> porCliente(@PathVariable Long clienteId) {
        return reservaService.buscarPorCliente(clienteId);
    }

    @GetMapping("/buscar")
    public List<Reserva> porRangoYEstado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam EstadoReserva estado) {
        return reservaService.buscarPorRangoYEstado(inicio, fin, estado);
    }

    @PostMapping("/{id}/confirmar-y-pagar")
    public Reserva confirmarYPagar(@PathVariable Long id, @RequestBody Pago pago) {
        return reservaService.confirmarYPagar(id, pago);
    }
}
