package com.recovr.backend.config;

import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sin DEMO_PASSWORD no hay cuentas demo utilizables: no se crean y las que existían (con las contraseñas fijas que
 * estuvieron publicadas en el repositorio) quedan desactivadas.
 */
@SpringBootTest(properties = {
        "recovr.seed.catalogo=true",
        "recovr.seed.demo-data=true",
        "recovr.seed.dias-historial=1",
        "recovr.seed.demo-password=",
        "spring.datasource.url=jdbc:h2:mem:recovr_demosinclave;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class DemoSinClaveTest {

    @Autowired private DemoDataSeeder seeder;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("No se crean cuentas demo y una cuenta demo antigua queda desactivada")
    void sinClaveNoHayCuentasDemo() throws Exception {
        assertThat(usuarioRepository.findAll()).noneMatch(u -> u.getCorreo().endsWith(".demo@example.com"));

        Usuario antigua = new Usuario();
        antigua.setCorreo("admin.demo@recovr.test");
        antigua.setPassword("$2a$10$hashDeUnaClavePublicadaAntes................");
        antigua.setRol(Rol.ADMIN);
        usuarioRepository.save(antigua);
        Usuario actual = new Usuario();
        actual.setCorreo("admin.demo@example.com");
        actual.setPassword("$2a$10$otroHash.........................................");
        actual.setRol(Rol.ADMIN);
        usuarioRepository.save(actual);

        seeder.run();

        assertThat(usuarioRepository.findByCorreo("admin.demo@recovr.test").orElseThrow().isActivo()).isFalse();
        assertThat(usuarioRepository.findByCorreo("admin.demo@example.com").orElseThrow().isActivo()).isFalse();
    }
}
