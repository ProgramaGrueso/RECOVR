package com.recovr.backend.service;

import com.recovr.backend.dto.UsuarioDtos.*;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Gestión de accesos por el ADMIN. Quién crea a quién:
 * <ul>
 *   <li>CLIENTE: se registra solo en el sitio (/api/auth/registro).</li>
 *   <li>ADMIN, RECEPCIONISTA y ESPECIALISTA: los crea un ADMIN aquí. El sistema genera una clave temporal
 *       (se muestra una sola vez) y el usuario debe cambiarla en su primer ingreso.</li>
 *   <li>El primer ADMIN nace por configuración (variables ADMIN_EMAIL y ADMIN_PASSWORD), nunca por un endpoint público.</li>
 * </ul>
 * Reglas: una ESPECIALISTA se vincula a una terapeuta que aún no tenga acceso; un admin no puede desactivarse a sí
 * mismo ni desactivar al último ADMIN activo.
 */
@Service
public class UsuarioService {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                          EmpleadoRepository empleadoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol rol) {
        return usuarioRepository.findAllByOrderByRolAscNombreAsc().stream()
                .filter(u -> rol == null || u.getRol() == rol).map(this::aRespuesta).toList();
    }

    @Transactional
    public UsuarioNuevoResponse crear(NuevoUsuarioRequest request) {
        if (request.rol() == Rol.CLIENTE) {
            throw new IllegalArgumentException("Los clientes se registran solos en el sitio; aquí se crea personal");
        }
        String correo = request.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        Empleado terapeuta = null;
        if (request.rol() == Rol.ESPECIALISTA) {
            if (request.empleadoId() == null) {
                throw new IllegalArgumentException("Una cuenta ESPECIALISTA debe vincularse a una terapeuta (empleadoId)");
            }
            terapeuta = empleadoRepository.findById(request.empleadoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", request.empleadoId()));
            if (terapeuta.getUsuarioId() != null) {
                throw new IllegalStateException(terapeuta.getNombre() + " ya tiene un acceso asignado");
            }
        } else if (request.empleadoId() != null) {
            throw new IllegalArgumentException("Solo una cuenta ESPECIALISTA se vincula a una terapeuta");
        }
        String clave = claveTemporal();
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre(request.nombre().trim());
        u.setRol(request.rol());
        u.setPassword(passwordEncoder.encode(clave));
        u.setDebeCambiarClave(true);
        u = usuarioRepository.save(u);
        if (terapeuta != null) {
            terapeuta.setUsuarioId(u.getId());
            empleadoRepository.save(terapeuta);
        }
        return new UsuarioNuevoResponse(aRespuesta(u), clave);
    }

    @Transactional
    public UsuarioResponse cambiarActivo(Long id, boolean activo, String correoActual) {
        Usuario u = buscar(id);
        if (!activo) {
            if (u.getCorreo().equalsIgnoreCase(correoActual)) {
                throw new IllegalStateException("No puedes desactivar tu propia cuenta");
            }
            if (u.getRol() == Rol.ADMIN && u.isActivo() && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
                throw new IllegalStateException("No se puede desactivar al último administrador activo");
            }
        }
        u.setActivo(activo);
        return aRespuesta(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioNuevoResponse restablecerClave(Long id) {
        Usuario u = buscar(id);
        String clave = claveTemporal();
        u.setPassword(passwordEncoder.encode(clave));
        u.setDebeCambiarClave(true);
        return new UsuarioNuevoResponse(aRespuesta(usuarioRepository.save(u)), clave);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    private UsuarioResponse aRespuesta(Usuario u) {
        String vinculo = "—";
        Long empleadoId = null;
        if (u.getRol() == Rol.ESPECIALISTA) {
            Empleado e = empleadoRepository.findByUsuarioId(u.getId()).orElse(null);
            vinculo = e != null ? "Terapeuta " + e.getNombre() : "Sin terapeuta vinculada";
            empleadoId = e != null ? e.getId() : null;
        } else if (u.getRol() == Rol.CLIENTE) {
            Cliente c = clienteRepository.findByUsuarioId(u.getId()).orElse(null);
            vinculo = c != null ? "Cliente CLI-" + c.getId() : "Sin ficha de cliente";
        }
        return new UsuarioResponse(u.getId(), u.getCorreo(), u.getNombre() != null ? u.getNombre() : u.getCorreo(),
                u.getRol().name(), u.isActivo(), u.isDebeCambiarClave(), vinculo, empleadoId);
    }

    /** 12 caracteres sin ambiguos (sin 0/O, 1/l/I), con al menos una letra y un número. */
    static String claveTemporal() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        sb.setCharAt(RANDOM.nextInt(12), "23456789".charAt(RANDOM.nextInt(8)));
        sb.setCharAt(RANDOM.nextInt(12), "ABCDEFGHJKLMNPQRSTUVWXYZ".charAt(RANDOM.nextInt(24)));
        return sb.toString().chars().anyMatch(Character::isDigit) && sb.chars().anyMatch(Character::isLetter)
                ? sb.toString() : claveTemporal();
    }
}
