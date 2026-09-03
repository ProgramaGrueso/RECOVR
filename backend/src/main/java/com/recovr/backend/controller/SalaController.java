package com.recovr.backend.controller;

import com.recovr.backend.entity.Sala;
import com.recovr.backend.service.SalaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    @Autowired
    private SalaService salaService;

    @GetMapping
    public List<Sala> listar() {
        return salaService.listarTodos();
    }

    @GetMapping("/{id}")
    public Sala obtener(@PathVariable Long id) {
        return salaService.buscarPorId(id);
    }

    @PostMapping
    public Sala crear(@RequestBody Sala sala) {
        return salaService.crear(sala);
    }

    @PutMapping("/{id}")
    public Sala actualizar(@PathVariable Long id, @RequestBody Sala sala) {
        return salaService.actualizar(id, sala);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        salaService.eliminar(id);
    }

    @GetMapping("/disponibles")
    public List<Sala> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return salaService.buscarDisponibles(inicio, fin);
    }
}
