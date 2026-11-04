package com.recovr.backend.service;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

/**
 * Traduce el usuario autenticado (su correo, que viene del JWT) a su ficha de cliente o de terapeuta. Antes cada
 * servicio repetía esta búsqueda.
 */
@Service
public class UsuarioActualService {

    static final String SIN_TERAPEUTA = "Tu usuario no está vinculado a ninguna terapeuta";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;

    public UsuarioActualService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                                EmpleadoRepository empleadoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
    }

    public Usuario usuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    /** Ficha de cliente del usuario; 400 si la cuenta no es de un cliente. */
    public Cliente cliente(String correo) {
        return clienteRepository.findByUsuarioId(usuario(correo).getId())
                .orElseThrow(() -> new IllegalStateException("El usuario no tiene un cliente asociado"));
    }

    public Long clienteId(String correo) {
        return cliente(correo).getId();
    }

    /** Ficha de terapeuta del usuario; 400 con {@code siNoVinculado} si no está vinculada a ninguna. */
    public Empleado empleado(String correo, String siNoVinculado) {
        return empleadoRepository.findByUsuarioId(usuario(correo).getId())
                .orElseThrow(() -> new IllegalStateException(siNoVinculado));
    }

    public Empleado empleado(String correo) {
        return empleado(correo, SIN_TERAPEUTA);
    }
}
