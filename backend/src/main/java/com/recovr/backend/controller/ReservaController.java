package com.recovr.backend.controller;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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
    public Reserva crear(@RequestBody Reserva reserva, Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return reservaService.crearParaUsuarioAutenticado(reserva, authentication.getName(), esAdmin);
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
    public List<Reserva> porCliente(@PathVariable Long clienteId, Authentication authentication) {
        return reservaService.buscarPorClienteParaUsuario(clienteId, authentication.getName(), esGestionDeReservas(authentication));
    }

    @GetMapping("/mias")
    public List<Reserva> misReservas(Authentication authentication) {
        return reservaService.buscarMisReservas(authentication.getName());
    }

    @GetMapping("/buscar")
    public List<Reserva> porRangoYEstado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam EstadoReserva estado) {
        return reservaService.buscarPorRangoYEstado(inicio, fin, estado);
    }

    @PostMapping("/{id}/confirmar-y-pagar")
    public Reserva confirmarYPagar(@PathVariable Long id, @RequestBody Pago pago, Authentication authentication) {
        return reservaService.confirmarYPagarParaUsuario(id, pago, authentication.getName(), esGestionDeReservas(authentication));
    }

    private boolean esGestionDeReservas(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_RECEPCIONISTA"));
    }
}
