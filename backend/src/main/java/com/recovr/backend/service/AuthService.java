package com.recovr.backend.service;

import com.recovr.backend.dto.AuthResponse;
import com.recovr.backend.dto.LoginRequest;
import com.recovr.backend.dto.RegistroRequest;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.exception.LoginBloqueadoException;
import com.recovr.backend.dto.PerfilResponse;
import com.recovr.backend.dto.UsuarioDtos.CambiarClaveRequest;
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
        // Si recepción ya lo había registrado en mostrador con ese correo, se vincula su ficha en lugar de duplicarla:
        // conserva su historial, sus puntos y su ficha (el correo del cliente es único).
        Cliente cliente = clienteRepository.findByCorreo(request.correo()).orElse(null);
        if (cliente != null && cliente.getUsuarioId() != null) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        if (cliente == null) {
            cliente = new Cliente();
            cliente.setNombre(request.nombre());
            cliente.setCorreo(request.correo());
            cliente.setTelefono(request.telefono());
        }
        cliente.setUsuarioId(usuario.getId());
        clienteRepository.save(cliente);
        return respuesta(usuario);
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
        if (!usuario.isActivo()) {
            // Mismo mensaje que una clave incorrecta: no se revela qué cuentas existen ni cuáles están desactivadas
            loginAttemptService.registrarFallo(request.correo(), ip, ahora);
            auditoriaService.registrar(usuario.getCorreo(), usuario.getRol().name(), "LOGIN_FALLIDO", "/api/auth/login", 401, ip, "Cuenta desactivada");
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
                clienteId, empleadoId, telefono, usuario.isDebeCambiarClave());
    }

    /**
     * Cambia la contraseña del usuario autenticado. Exige la actual (un fallo cuenta para el bloqueo por intentos),
     * una nueva distinta que cumpla la política, y apaga la marca de "clave temporal".
     */
    @Transactional
    public void cambiarClave(String correo, CambiarClaveRequest request, String ip) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Instant ahora = Instant.now();
        if (loginAttemptService.estaBloqueado(correo, ip, ahora)) {
            throw new LoginBloqueadoException("Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar.");
        }
        if (!passwordEncoder.matches(request.claveActual(), usuario.getPassword())) {
            loginAttemptService.registrarFallo(correo, ip, ahora);
            auditoriaService.registrar(correo, usuario.getRol().name(), "CAMBIO_CLAVE_FALLIDO", "/api/auth/cambiar-clave", 400, ip, null);
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.claveNueva(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña nueva debe ser distinta de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(request.claveNueva()));
        usuario.setDebeCambiarClave(false);
        usuarioRepository.save(usuario);
        loginAttemptService.registrarExito(correo, ip);
        auditoriaService.registrar(correo, usuario.getRol().name(), "CAMBIO_CLAVE", "/api/auth/cambiar-clave", 200, ip, null);
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
