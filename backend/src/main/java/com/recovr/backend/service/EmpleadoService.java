package com.recovr.backend.service;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.repository.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id " + id));
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
        empleadoRepository.deleteById(id);
    }
}
