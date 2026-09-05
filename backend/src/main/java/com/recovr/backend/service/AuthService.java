package com.recovr.backend.service;

import com.recovr.backend.dto.AuthResponse;
import com.recovr.backend.dto.CrearUsuarioRequest;
import com.recovr.backend.dto.LoginRequest;
import com.recovr.backend.dto.RegistroRequest;
import com.recovr.backend.dto.UsuarioCreadoResponse;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.security.CustomUserDetailsService;
import com.recovr.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                       PasswordEncoder passwordEncoder, CustomUserDetailsService userDetailsService,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrarCliente(RegistroRequest request) {
        Usuario usuario = crearUsuario(request, Rol.CLIENTE);
        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre());
        cliente.setCorreo(request.correo());
        cliente.setTelefono(request.telefono());
        cliente.setUsuarioId(usuario.getId());
        clienteRepository.save(cliente);
        return respuesta(usuario);
    }

    public AuthResponse registrarPrimerAdmin(RegistroRequest request) {
        if (usuarioRepository.countByRol(Rol.ADMIN) > 0) {
            throw new IllegalStateException("Ya existe un administrador. Un admin autenticado debe crear los siguientes.");
        }
        return respuesta(crearUsuario(request, Rol.ADMIN));
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new BadCredentialsException("Correo o contrasena incorrectos"));
        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Correo o contrasena incorrectos");
        }
        return respuesta(usuario);
    }

    public UsuarioCreadoResponse crearUsuarioPersonal(CrearUsuarioRequest request) {
        if (request.rol() == Rol.CLIENTE) {
            throw new IllegalArgumentException("Los clientes deben registrarse mediante /api/auth/registro");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(request.correo());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario = usuarioRepository.save(usuario);
        return new UsuarioCreadoResponse(usuario.getId(), usuario.getCorreo(), usuario.getRol().name());
    }

    private Usuario crearUsuario(RegistroRequest request, Rol rol) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(request.correo());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getCorreo());
        return new AuthResponse(jwtService.generarToken(userDetails), "Bearer", usuario.getRol().name(), usuario.getCorreo());
    }
}
