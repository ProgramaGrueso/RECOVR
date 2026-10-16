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
    private com.recovr.backend.repository.UsuarioRepository usuarioRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setUsuarioRepository(com.recovr.backend.repository.UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

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
        String correoAnterior = cliente.getCorreo();
        cliente.setNombre(datos.getNombre());
        cliente.setCorreo(datos.getCorreo());
        cliente.setTelefono(datos.getTelefono());
        sincronizarUsuario(cliente, correoAnterior);
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

    /**
     * El correo de un cliente con cuenta es también su usuario de acceso: si cambia en la ficha, cambia en la cuenta
     * (si no, el cliente quedaría con una ficha y un acceso con correos distintos). Un correo ya usado por otra cuenta
     * se rechaza. El token anterior deja de valer porque identifica al usuario por su correo.
     */
    private void sincronizarUsuario(Cliente cliente, String correoAnterior) {
        if (usuarioRepository == null || cliente.getUsuarioId() == null) {
            return;
        }
        var usuario = usuarioRepository.findById(cliente.getUsuarioId()).orElse(null);
        if (usuario == null) {
            return;
        }
        if (!cliente.getCorreo().equalsIgnoreCase(usuario.getCorreo())) {
            if (usuarioRepository.existsByCorreo(cliente.getCorreo())) {
                throw new IllegalArgumentException("Ya existe un usuario con ese correo");
            }
            usuario.setCorreo(cliente.getCorreo());
        }
        usuario.setNombre(cliente.getNombre());
        usuarioRepository.save(usuario);
    }
}
