package com.recovr.backend.service;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

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
        empleadoRepository.deleteById(id);
    }

    public List<Empleado> buscarDisponibles(LocalDateTime inicio, LocalDateTime fin) {
        ReservaService.validarRango(inicio, fin);
        return empleadoRepository.buscarDisponibles(inicio, fin);
    }
}
