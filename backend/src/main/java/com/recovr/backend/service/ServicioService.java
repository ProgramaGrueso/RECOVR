package com.recovr.backend.service;

import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioService {

    private static final java.util.List<EstadoReserva> RESERVAS_ACTIVAS =
            java.util.List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private final ServicioRepository servicioRepository;
    private final ReservaRepository reservaRepository;

    public ServicioService(ServicioRepository servicioRepository, ReservaRepository reservaRepository) {
        this.servicioRepository = servicioRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Servicio> listarTodos() {
        return servicioRepository.findAll();
    }

    public Servicio buscarPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio", id));
    }

    public Servicio crear(Servicio servicio) {
        return servicioRepository.save(servicio);
    }

    public Servicio actualizar(Long id, Servicio datos) {
        Servicio servicio = buscarPorId(id);
        servicio.setNombre(datos.getNombre());
        servicio.setDuracionMinutos(datos.getDuracionMinutos());
        servicio.setTiempoLimpiezaMinutos(datos.getTiempoLimpiezaMinutos());
        servicio.setPrecio(datos.getPrecio());
        return servicioRepository.save(servicio);
    }

    public void eliminar(Long id) {
        if (!servicioRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Servicio", id);
        }
        if (reservaRepository.existsByServicioIdAndEstadoIn(id, RESERVAS_ACTIVAS)) {
            throw new IllegalStateException("No se puede eliminar el servicio: tiene reservas activas (PENDIENTE o CONFIRMADA)");
        }
        if (reservaRepository.existsByServicioId(id)) {
            throw new IllegalStateException("No se puede eliminar el servicio: tiene historial de reservas");
        }
        servicioRepository.deleteById(id);
    }
}
