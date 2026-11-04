package com.recovr.backend.exception;

import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.security.CustomUserDetailsService;
import com.recovr.backend.security.JwtService;
import com.recovr.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug: sin manejador para los errores de bloqueo (MySQL "Lock wait timeout" con dos reservas simultáneas) ni para
 * excepciones inesperadas, la API respondía 500 con el formato por defecto de Spring o propagaba la excepción.
 * Ahora un bloqueo que no se pudo obtener es un 409 reintentable y cualquier otro error, un 500 con mensaje genérico
 * que no filtra detalles internos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ErroresApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;
    @MockBean private ReservaService reservaService;

    private String token;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario();
        u.setCorreo("admin.errores@example.com");
        u.setPassword(passwordEncoder.encode("Clave1234"));
        u.setRol(Rol.ADMIN);
        usuarioRepository.save(u);
        token = jwtService.generarToken(userDetailsService.loadUserByUsername(u.getCorreo()));
    }

    @Test
    @DisplayName("Un bloqueo de fila que no se obtiene a tiempo responde 409 (reintentable), no 500")
    void bloqueoEsConflicto() throws Exception {
        given(reservaService.buscarPorId(any())).willThrow(new CannotAcquireLockException("Lock wait timeout exceeded; try restarting transaction"));

        mockMvc.perform(get("/api/db/reservas/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(not(containsString("Lock wait"))));
    }

    @Test
    @DisplayName("Un error inesperado responde 500 con el formato de la API y sin detalles internos")
    void errorInesperadoNoFiltraDetalles() throws Exception {
        given(reservaService.buscarPorId(any())).willThrow(new NullPointerException("campo secreto interno"));

        mockMvc.perform(get("/api/db/reservas/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(content().string(not(containsString("secreto"))))
                .andExpect(content().string(not(containsString("NullPointer"))));
    }

    @Test
    @DisplayName("Un rol sin permiso recibe el mismo formato de error que el resto de la API (403 con timestamp y mensaje)")
    void prohibidoConFormatoComun() throws Exception {
        Usuario cliente = new Usuario();
        cliente.setCorreo("cliente.errores@example.com");
        cliente.setPassword(passwordEncoder.encode("Clave1234"));
        cliente.setRol(Rol.CLIENTE);
        usuarioRepository.save(cliente);
        String tokenCliente = jwtService.generarToken(userDetailsService.loadUserByUsername(cliente.getCorreo()));

        mockMvc.perform(get("/api/db/reservas").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Acceso denegado"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
