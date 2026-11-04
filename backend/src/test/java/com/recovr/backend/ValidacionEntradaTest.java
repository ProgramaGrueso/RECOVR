package com.recovr.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug: los textos libres no tenían longitud máxima. Un nombre o teléfono más largo que la columna llegaba a la base,
 * fallaba como violación de integridad y la API respondía 409 "registro duplicado o en uso", un mensaje falso.
 * Ahora Bean Validation lo rechaza con 400 e indica el campo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValidacionEntradaTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Registro con nombre o teléfono demasiado largos: 400 con el campo, no 409")
    void registroConTextosLargos() throws Exception {
        String nombreLargo = "N".repeat(300);
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombreLargo + "\",\"correo\":\"largo@example.com\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"tel@example.com\",\"password\":\"Clave1234\",\"telefono\":\"" + "9".repeat(300) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.telefono").exists());
    }

    @Test
    @DisplayName("Correo de más de 254 caracteres: 400")
    void correoDemasiadoLargo() throws Exception {
        String correo = "a".repeat(250) + "@example.com";
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"" + correo + "\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.correo").exists());
    }
}
