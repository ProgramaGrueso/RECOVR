package com.recovr.backend.controller;

import com.recovr.backend.dto.FichaDtos.*;
import com.recovr.backend.service.FichaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fichas")
@Tag(name = "Fichas de clientes", description = "Ficha confidencial y notas de sesión. Solo ESPECIALISTA, y solo de sus clientes.")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @Operation(summary = "Mis clientes", description = "Clientes con los que la terapeuta autenticada tiene o tuvo reservas.")
    @GetMapping("/mis-clientes")
    public List<MiClienteResponse> misClientes(Authentication auth) {
        return fichaService.misClientes(auth.getName());
    }

    @Operation(summary = "Ficha de un cliente", description = "403 si la terapeuta nunca atendió a ese cliente.")
    @GetMapping("/cliente/{clienteId}")
    public FichaResponse obtener(@PathVariable Long clienteId, Authentication auth) {
        return fichaService.obtener(auth.getName(), clienteId);
    }

    @Operation(summary = "Crear o actualizar la ficha de un cliente")
    @PutMapping("/cliente/{clienteId}")
    public FichaResponse guardar(@PathVariable Long clienteId, @Valid @RequestBody FichaRequest request, Authentication auth) {
        return fichaService.guardar(auth.getName(), clienteId, request);
    }

    @Operation(summary = "Agregar una nota de sesión")
    @PostMapping("/cliente/{clienteId}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    public FichaResponse agregarNota(@PathVariable Long clienteId, @Valid @RequestBody NotaRequest request, Authentication auth) {
        return fichaService.agregarNota(auth.getName(), clienteId, request);
    }

    @Operation(summary = "Eliminar una nota propia")
    @DeleteMapping("/notas/{notaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarNota(@PathVariable Long notaId, Authentication auth) {
        fichaService.eliminarNota(auth.getName(), notaId);
    }
}
