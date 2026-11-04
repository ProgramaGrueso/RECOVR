package com.recovr.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS con lista blanca configurable (CORS_ALLOWED_ORIGINS). Antes el filtro aceptaba por expresión regular cualquier
 * puerto de localhost y CUALQUIER subdominio *.vercel.app (es decir, cualquier proyecto de cualquier persona en Vercel)
 * y respondía 200 a cualquier preflight.
 */
@SpringBootTest(properties = "recovr.cors.allowed-origins=https://recovr.example.com, http://localhost:4200")
@AutoConfigureMockMvc
class CorsListaBlancaTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Un origen de la lista recibe Access-Control-Allow-Origin con su valor exacto")
    void origenPermitido() throws Exception {
        mockMvc.perform(get("/api/publico/terapeutas").header("Origin", "https://recovr.example.com"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://recovr.example.com"));
        mockMvc.perform(options("/api/auth/login").header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    @DisplayName("Un proyecto ajeno en Vercel u otro puerto de localhost ya no es aceptado")
    void origenAjeno() throws Exception {
        mockMvc.perform(get("/api/publico/terapeutas").header("Origin", "https://atacante.vercel.app"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mockMvc.perform(get("/api/publico/terapeutas").header("Origin", "http://localhost:6666"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("El preflight de un origen no permitido se rechaza con 403")
    void preflightRechazado() throws Exception {
        mockMvc.perform(options("/api/auth/login").header("Origin", "https://atacante.vercel.app")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
