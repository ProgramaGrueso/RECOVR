package com.recovr.backend.security;

import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización explícita: toda ruta tiene una regla por rol y lo que no está en la lista se deniega (antes cualquier
 * ruta no listada quedaba abierta a cualquier usuario autenticado, de cualquier rol).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DenegarPorDefectoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;

    private String token(Rol rol) {
        Usuario u = new Usuario();
        u.setCorreo(rol.name().toLowerCase() + ".denegar@example.com");
        u.setPassword(passwordEncoder.encode("Clave1234"));
        u.setRol(rol);
        usuarioRepository.save(u);
        return "Bearer " + jwtService.generarToken(userDetailsService.loadUserByUsername(u.getCorreo()));
    }

    @Test
    @DisplayName("Una ruta no listada se deniega aunque el usuario esté autenticado")
    void rutaNoListadaSeDeniega() throws Exception {
        String cliente = token(Rol.CLIENTE);
        mockMvc.perform(get("/api/ruta-nueva-sin-regla").header("Authorization", cliente)).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/env").header("Authorization", cliente)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Las rutas de la propia sesión siguen abiertas a cualquier rol autenticado")
    void rutasDeSesion() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", token(Rol.ESPECIALISTA))).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
