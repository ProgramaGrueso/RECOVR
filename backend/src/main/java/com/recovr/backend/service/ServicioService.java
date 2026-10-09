package com.recovr.backend.service;

import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioService {

    @Autowired
    private ServicioRepository servicioRepository;

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
        servicioRepository.deleteById(id);
    }
}
