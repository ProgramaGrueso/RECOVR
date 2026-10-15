package com.recovr.backend.service;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final com.recovr.backend.repository.ReservaRepository reservaRepository;

    public ClienteService(ClienteRepository clienteRepository, com.recovr.backend.repository.ReservaRepository reservaRepository) {
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    public Cliente crear(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente datos) {
        Cliente cliente = buscarPorId(id);
        cliente.setNombre(datos.getNombre());
        cliente.setCorreo(datos.getCorreo());
        cliente.setTelefono(datos.getTelefono());
        return clienteRepository.save(cliente);
    }

    public void eliminar(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Cliente", id);
        }
        if (reservaRepository.existsByClienteId(id)) {
            throw new IllegalStateException("No se puede eliminar el cliente: tiene reservas registradas (historial)");
        }
        clienteRepository.deleteById(id);
    }
}
