package com.recovr.backend.service;

import com.recovr.backend.entity.Sala;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalaService {

    @Autowired
    private SalaRepository salaRepository;

    public List<Sala> listarTodos() {
        return salaRepository.findAll();
    }

    public Sala buscarPorId(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sala", id));
    }

    public Sala crear(Sala sala) {
        return salaRepository.save(sala);
    }

    public Sala actualizar(Long id, Sala datos) {
        Sala sala = buscarPorId(id);
        sala.setNombre(datos.getNombre());
        sala.setCapacidad(datos.getCapacidad());
        return salaRepository.save(sala);
    }

    public void eliminar(Long id) {
        if (!salaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Sala", id);
        }
        salaRepository.deleteById(id);
    }

    public List<Sala> buscarDisponibles(LocalDateTime inicio, LocalDateTime fin) {
        ReservaService.validarRango(inicio, fin);
        return salaRepository.buscarDisponibles(inicio, fin);
    }
}
