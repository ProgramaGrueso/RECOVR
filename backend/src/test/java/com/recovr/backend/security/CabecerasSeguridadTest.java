package com.recovr.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/** Cabeceras de seguridad en las respuestas de la API. */
@SpringBootTest
@AutoConfigureMockMvc
class CabecerasSeguridadTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("La API responde con CSP restrictiva, nosniff, sin marcos, sin referrer y sin caché")
    void cabecerasEnLaApi() throws Exception {
        mockMvc.perform(get("/api/publico/terapeutas"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                .andExpect(header().exists("Cache-Control"));
    }

    @Test
    @DisplayName("Swagger UI no recibe la CSP de la API (necesita sus scripts)")
    void swaggerSinCspDeApi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }
}
