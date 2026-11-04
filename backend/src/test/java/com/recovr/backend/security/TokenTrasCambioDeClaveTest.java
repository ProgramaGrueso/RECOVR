package com.recovr.backend.security;

import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un token emitido antes de cambiar o restablecer la contraseña deja de valer al instante. Antes seguía vigente hasta
 * su expiración (24 h): quien robara un token conservaba el acceso aunque la víctima cambiara su clave.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TokenTrasCambioDeClaveTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;

    private String token(String correo) {
        return "Bearer " + jwtService.generarToken(userDetailsService.loadUserByUsername(correo));
    }

    private Usuario usuario(String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode("Clave1234"));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    @Test
    @DisplayName("Tras cambiar la contraseña, el token anterior responde 401 y el nuevo funciona")
    void cambioDeClaveInvalidaTokensAnteriores() throws Exception {
        usuario("cambia@example.com", Rol.CLIENTE);
        String viejo = token("cambia@example.com");
        mockMvc.perform(get("/api/auth/me").header("Authorization", viejo)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/cambiar-clave").header("Authorization", viejo).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"Clave1234\",\"claveNueva\":\"OtraClave987\"}"))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/auth/me").header("Authorization", viejo)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", token("cambia@example.com"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Si un ADMIN restablece la clave de una cuenta, los tokens de esa cuenta dejan de valer")
    void restablecerInvalidaTokens() throws Exception {
        usuario("admin.tokens@example.com", Rol.ADMIN);
        Usuario recep = usuario("recep.tokens@example.com", Rol.RECEPCIONISTA);
        String tokenRecep = token("recep.tokens@example.com");

        mockMvc.perform(post("/api/usuarios/" + recep.getId() + "/restablecer-clave")
                        .header("Authorization", token("admin.tokens@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", tokenRecep)).andExpect(status().isUnauthorized());
    }
}
