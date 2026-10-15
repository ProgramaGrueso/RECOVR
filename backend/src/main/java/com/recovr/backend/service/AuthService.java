package com.recovr.backend.service;

import com.recovr.backend.dto.AuthResponse;
import com.recovr.backend.dto.CrearUsuarioRequest;
import com.recovr.backend.dto.LoginRequest;
import com.recovr.backend.dto.RegistroRequest;
import com.recovr.backend.dto.UsuarioCreadoResponse;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.exception.LoginBloqueadoException;
import com.recovr.backend.dto.PerfilResponse;
import com.recovr.backend.security.LoginAttemptService;
import java.time.Instant;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
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
    private final EmpleadoRepository empleadoRepository;
    private final LoginAttemptService loginAttemptService;
    private final AuditoriaService auditoriaService;

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                       PasswordEncoder passwordEncoder, CustomUserDetailsService userDetailsService,
                       JwtService jwtService, EmpleadoRepository empleadoRepository,
                       LoginAttemptService loginAttemptService, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.empleadoRepository = empleadoRepository;
        this.loginAttemptService = loginAttemptService;
        this.auditoriaService = auditoriaService;
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
            return login(new LoginRequest(request.correo(), request.password()));
        }
        return respuesta(crearUsuario(request, Rol.ADMIN));
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, "interno");
    }

    /**
     * Inicia sesión con freno anti-adivinanza y auditoría. El mensaje de error es el mismo para un correo inexistente
     * y para una contraseña incorrecta, de modo que no se puede averiguar qué correos están registrados.
     */
    public AuthResponse login(LoginRequest request, String ip) {
        Instant ahora = Instant.now();
        if (loginAttemptService.estaBloqueado(request.correo(), ip, ahora)) {
            auditoriaService.registrar(request.correo(), "-", "LOGIN_BLOQUEADO", "/api/auth/login", 429, ip, "Demasiados intentos fallidos");
            throw new LoginBloqueadoException("Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar.");
        }
        Usuario usuario = usuarioRepository.findByCorreo(request.correo()).orElse(null);
        if (usuario == null || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            loginAttemptService.registrarFallo(request.correo(), ip, ahora);
            auditoriaService.registrar(request.correo(), "-", "LOGIN_FALLIDO", "/api/auth/login", 401, ip, null);
            throw new BadCredentialsException("Correo o contrasena incorrectos");
        }
        loginAttemptService.registrarExito(request.correo(), ip);
        auditoriaService.registrar(usuario.getCorreo(), usuario.getRol().name(), "LOGIN_OK", "/api/auth/login", 200, ip, null);
        return respuesta(usuario);
    }

    /** Perfil de la sesión actual: nombre, rol y, según el rol, la ficha de cliente o de terapeuta vinculada. */
    @Transactional(readOnly = true)
    public PerfilResponse perfil(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Long clienteId = null;
        Long empleadoId = null;
        String telefono = null;
        String nombre = usuario.getNombre();
        if (usuario.getRol() == Rol.CLIENTE) {
            Cliente c = clienteRepository.findByUsuarioId(usuario.getId()).orElse(null);
            if (c != null) {
                clienteId = c.getId();
                telefono = c.getTelefono();
                nombre = nombre != null ? nombre : c.getNombre();
            }
        } else if (usuario.getRol() == Rol.ESPECIALISTA) {
            Empleado e = empleadoRepository.findByUsuarioId(usuario.getId()).orElse(null);
            if (e != null) {
                empleadoId = e.getId();
                telefono = e.getTelefono();
                nombre = nombre != null ? nombre : e.getNombre();
            }
        }
        return new PerfilResponse(usuario.getCorreo(), usuario.getRol().name(), nombre != null ? nombre : usuario.getCorreo(),
                clienteId, empleadoId, telefono);
    }

    @Transactional
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
        usuario.setNombre(request.nombre());
        usuario = usuarioRepository.save(usuario);
        vincularEspecialista(usuario, request);
        return new UsuarioCreadoResponse(usuario.getId(), usuario.getCorreo(), usuario.getRol().name());
    }

    // Un ESPECIALISTA puede vincularse a su ficha de Empleado para consultar su agenda.
    private void vincularEspecialista(Usuario usuario, CrearUsuarioRequest request) {
        if (request.empleadoId() == null) {
            return;
        }
        if (request.rol() != Rol.ESPECIALISTA) {
            throw new IllegalArgumentException("Solo un usuario ESPECIALISTA puede vincularse a un empleado");
        }
        Empleado empleado = empleadoRepository.findById(request.empleadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", request.empleadoId()));
        if (empleado.getUsuarioId() != null) {
            throw new IllegalStateException("El empleado " + empleado.getId() + " ya tiene un usuario vinculado");
        }
        empleado.setUsuarioId(usuario.getId());
        empleadoRepository.save(empleado);
    }

    private Usuario crearUsuario(RegistroRequest request, Rol rol) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(request.correo());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(rol);
        usuario.setNombre(request.nombre());
        return usuarioRepository.save(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getCorreo());
        return new AuthResponse(jwtService.generarToken(userDetails), "Bearer", usuario.getRol().name(), usuario.getCorreo(),
                usuario.getNombre());
    }
}
