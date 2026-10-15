package com.recovr.backend.config;

import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el PRIMER administrador a partir de la configuración (ADMIN_EMAIL, ADMIN_PASSWORD y opcional ADMIN_NOMBRE),
 * solo si todavía no existe ningún ADMIN. Reemplaza al antiguo endpoint público "registro-admin", con el que cualquiera
 * podía reclamar la administración de un sistema vacío. La clave inicial debe cambiarse en el primer ingreso.
 * Los demás accesos del personal los crea ese administrador desde el panel (módulo "Usuarios y accesos").
 */
@Component
@Order(0)
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;
    private final String nombre;

    public AdminBootstrap(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          @Value("${recovr.bootstrap.admin-email:}") String correo,
                          @Value("${recovr.bootstrap.admin-password:}") String password,
                          @Value("${recovr.bootstrap.admin-nombre:Administrador}") String nombre) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo.trim().toLowerCase();
        this.password = password;
        this.nombre = nombre;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.countByRol(Rol.ADMIN) > 0) {
            return;
        }
        if (correo.isBlank() || password.isBlank()) {
            log.warn("No existe ningún administrador. Define ADMIN_EMAIL y ADMIN_PASSWORD para crear el primero "
                    + "(o activa SEED_DEMO_DATA=true para las cuentas de demostración).");
            return;
        }
        if (password.length() < 8 || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new IllegalStateException("ADMIN_PASSWORD no cumple la política: mínimo 8 caracteres, con letras y números");
        }
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setNombre(nombre);
        u.setRol(Rol.ADMIN);
        u.setPassword(passwordEncoder.encode(password));
        u.setDebeCambiarClave(true);
        usuarioRepository.save(u);
        log.info("Administrador inicial creado: {} (deberá cambiar la contraseña al ingresar).", correo);
    }
}
