package com.recovr.backend.service;

import com.recovr.backend.entity.Sala;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalaService {

    private static final java.util.List<EstadoReserva> RESERVAS_ACTIVAS =
            java.util.List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private final SalaRepository salaRepository;
    private final ReservaRepository reservaRepository;

    public SalaService(SalaRepository salaRepository, ReservaRepository reservaRepository) {
        this.salaRepository = salaRepository;
        this.reservaRepository = reservaRepository;
    }

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
        if (reservaRepository.existsBySalaIdAndEstadoIn(id, RESERVAS_ACTIVAS)) {
            throw new IllegalStateException("No se puede eliminar la sala: tiene reservas activas (PENDIENTE o CONFIRMADA)");
        }
        if (reservaRepository.existsBySalaId(id)) {
            throw new IllegalStateException("No se puede eliminar la sala: tiene historial de reservas");
        }
        salaRepository.deleteById(id);
    }

    public List<Sala> buscarDisponibles(LocalDateTime inicio, LocalDateTime fin) {
        ReservaService.validarRango(inicio, fin);
        return salaRepository.buscarDisponibles(inicio, fin);
    }
}
