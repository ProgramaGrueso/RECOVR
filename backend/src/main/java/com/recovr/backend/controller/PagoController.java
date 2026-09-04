package com.recovr.backend.controller;

import com.recovr.backend.entity.Pago;
import com.recovr.backend.service.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @GetMapping
    public List<Pago> listar() {
        return pagoService.listarTodos();
    }

    @GetMapping("/{id}")
    public Pago obtener(@PathVariable Long id) {
        return pagoService.buscarPorId(id);
    }

    @PostMapping
    public Pago crear(@RequestBody Pago pago) {
        return pagoService.crear(pago);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        pagoService.eliminar(id);
    }
}
