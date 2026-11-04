package com.recovr.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate limiting por IP. El freno anterior era por par (correo, IP): una misma IP podía probar contraseñas contra
 * cientos de correos distintos (credential stuffing) y crear cuentas sin límite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LimitesPorIpTest {

    @Autowired private MockMvc mockMvc;

    private static RequestPostProcessor desde(String ip) {
        return r -> { r.setRemoteAddr(ip); return r; };
    }

    @Test
    @DisplayName("Muchos logins fallidos desde una IP contra correos distintos terminan en 429")
    void credentialStuffingDesdeUnaIp() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_FALLOS_IP; i++) {
            mockMvc.perform(post("/api/auth/login").with(desde("203.0.113.7")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"victima" + i + "@example.com\",\"password\":\"Adivina123\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").with(desde("203.0.113.7")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"otra.victima@example.com\",\"password\":\"Adivina123\"}"))
                .andExpect(status().isTooManyRequests());
        // Otra IP no se ve afectada
        mockMvc.perform(post("/api/auth/login").with(desde("198.51.100.9")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"otra.victima@example.com\",\"password\":\"Adivina123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El registro de cuentas está limitado por IP")
    void registroLimitadoPorIp() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_REGISTROS_IP; i++) {
            mockMvc.perform(post("/api/auth/registro").with(desde("203.0.113.50")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Cuenta " + i + "\",\"correo\":\"masivo" + i + "@example.com\",\"password\":\"Clave1234\"}"))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/auth/registro").with(desde("203.0.113.50")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Una más\",\"correo\":\"masivo.extra@example.com\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
