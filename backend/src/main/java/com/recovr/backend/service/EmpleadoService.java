package com.recovr.backend.service;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpleadoService {

    private static final java.util.List<EstadoReserva> RESERVAS_ACTIVAS =
            java.util.List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private final EmpleadoRepository empleadoRepository;
    private final ReservaRepository reservaRepository;

    public EmpleadoService(EmpleadoRepository empleadoRepository, ReservaRepository reservaRepository) {
        this.empleadoRepository = empleadoRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Empleado> listarTodos() {
        return empleadoRepository.findAll();
    }

    public Empleado buscarPorId(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", id));
    }

    public Empleado crear(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }

    public Empleado actualizar(Long id, Empleado datos) {
        Empleado empleado = buscarPorId(id);
        empleado.setNombre(datos.getNombre());
        empleado.setEspecialidad(datos.getEspecialidad());
        empleado.setTelefono(datos.getTelefono());
        return empleadoRepository.save(empleado);
    }

    public void eliminar(Long id) {
        if (!empleadoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Empleado", id);
        }
        if (reservaRepository.existsByEmpleadoIdAndEstadoIn(id, RESERVAS_ACTIVAS)) {
            throw new IllegalStateException("No se puede eliminar al empleado: tiene reservas activas (PENDIENTE o CONFIRMADA)");
        }
        if (reservaRepository.existsByEmpleadoId(id)) {
            throw new IllegalStateException("No se puede eliminar al empleado: tiene historial de reservas");
        }
        empleadoRepository.deleteById(id);
    }

    public List<Empleado> buscarDisponibles(LocalDateTime inicio, LocalDateTime fin) {
        ReservaService.validarRango(inicio, fin);
        return empleadoRepository.buscarDisponibles(inicio, fin);
    }
}
