package com.recovr.backend.controller;

import com.recovr.backend.entity.Servicio;
import com.recovr.backend.service.ServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    @Autowired
    private ServicioService servicioService;

    @GetMapping
    public List<Servicio> listar() {
        return servicioService.listarTodos();
    }

    @GetMapping("/{id}")
    public Servicio obtener(@PathVariable Long id) {
        return servicioService.buscarPorId(id);
    }

    @PostMapping
    public Servicio crear(@RequestBody Servicio servicio) {
        return servicioService.crear(servicio);
    }

    @PutMapping("/{id}")
    public Servicio actualizar(@PathVariable Long id, @RequestBody Servicio servicio) {
        return servicioService.actualizar(id, servicio);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        servicioService.eliminar(id);
    }
}
