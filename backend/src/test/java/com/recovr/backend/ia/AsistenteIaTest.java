package com.recovr.backend.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.ServicioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Asistente de IA: el endpoint es público, sin clave responde 503 y las herramientas solo leen datos públicos. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AsistenteIaTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private IaHerramientas herramientas;
    @Autowired private AgenteIa agente;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private ObjectMapper mapper;

    @Test
    @DisplayName("El chat es público (sin JWT) y sin DEEPSEEK_API_KEY responde 503 con un mensaje útil")
    void chatPublicoSinClave() throws Exception {
        mockMvc.perform(get("/api/ia/estado")).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(post("/api/ia/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensajes\":[{\"rol\":\"user\",\"contenido\":\"Hola\"}]}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("listar_tratamientos devuelve la carta de la base con precio en soles")
    void listarTratamientos() throws Exception {
        Servicio s = new Servicio();
        s.setNombre("Masaje de prueba");
        s.setDuracionMinutos(60);
        s.setPrecio(new BigDecimal("180.00"));
        servicioRepository.save(s);

        JsonNode carta = mapper.readTree(herramientas.ejecutar("listar_tratamientos", "{}"));
        assertThat(carta.isArray()).isTrue();
        assertThat(carta.findValuesAsText("nombre")).contains("Masaje de prueba");
        assertThat(carta.findValuesAsText("precio_soles")).contains("180.00");
    }

    @Test
    @DisplayName("ver_turnos valida los argumentos y no consulta fechas pasadas ni lejanas")
    void verTurnosValida() throws Exception {
        assertThat(mapper.readTree(herramientas.ejecutar("ver_turnos", "{}")).has("error")).isTrue();
        String ayer = LocalDate.now(IaHerramientas.LIMA).minusDays(1).toString();
        assertThat(herramientas.ejecutar("ver_turnos", "{\"servicio_id\":1,\"terapeuta_id\":1,\"fecha\":\"" + ayer + "\"}"))
                .contains("ya pasó");
        String lejos = LocalDate.now(IaHerramientas.LIMA).plusDays(90).toString();
        assertThat(herramientas.ejecutar("ver_turnos", "{\"servicio_id\":1,\"terapeuta_id\":1,\"fecha\":\"" + lejos + "\"}"))
                .contains("anticipación");
        assertThat(mapper.readTree(herramientas.ejecutar("crear_reserva", "{}")).path("error").asText()).isNotBlank();
    }

    @Test
    @DisplayName("El prompt lleva los datos reales del negocio (horario, dirección, WhatsApp y reglas de reserva)")
    void promptConDatosDelNegocio() {
        String prompt = agente.promptSistema();
        assertThat(prompt).contains("RECOVR", "12:00", "23:00", "Av. Primavera 1250", "924 040 992", "15 minutos");
    }

    @Test
    @DisplayName("Las respuestas largas se cortan en el último punto")
    void recorta() {
        String largo = "Frase uno. ".repeat(80);
        assertThat(agente.recortar(largo).length()).isLessThanOrEqualTo(500);
        assertThat(agente.recortar(largo)).endsWith(".");
    }
}
