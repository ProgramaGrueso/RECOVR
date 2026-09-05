package com.recovr.controller;

import com.recovr.dto.CrearReservaRequest;
import com.recovr.model.EstadoReserva;
import com.recovr.model.Reserva;
import com.recovr.repository.InMemoryReservaRepository;
import com.recovr.repository.ReservaRepository;
import com.recovr.service.ReservaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservaControllerTest {

    private MockMvc mockMvc;
    private ReservaRepository reservaRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reservaRepository = new InMemoryReservaRepository();
        ReservaService reservaService = new ReservaService(reservaRepository);
        ReservaController reservaController = new ReservaController(reservaService);

        mockMvc = MockMvcBuilders.standaloneSetup(reservaController)
                .setControllerAdvice(new ReservaControllerAdvice())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/reservas debe registrar una reserva y retornar HTTP 201 Created")
    void debeCrearReservaYRetornar201() throws Exception {
        CrearReservaRequest request = new CrearReservaRequest(
                1L, 2L, 3L, 4L,
                LocalDateTime.of(2026, 9, 15, 10, 0),
                60, new BigDecimal("65.00")
        );

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.montoTotal").value(65.00));
    }

    @Test
    @DisplayName("POST /api/reservas debe retornar HTTP 409 Conflict cuando hay solapamiento de agenda")
    void debeRetornar409CuandoHayConflictoDeSolapamiento() throws Exception {
        // Reserva previa existente para el empleado 2L
        Reserva previa = new Reserva(
                null, 1L, 2L, 3L, 4L,
                LocalDateTime.of(2026, 9, 15, 10, 0),
                60, EstadoReserva.CONFIRMADA, new BigDecimal("65.00")
        );
        reservaRepository.save(previa);

        // Intento de nueva reserva en el mismo horario con el mismo especialista
        CrearReservaRequest requestSolapado = new CrearReservaRequest(
                2L, 2L, 3L, 5L,
                LocalDateTime.of(2026, 9, 15, 10, 30),
                60, new BigDecimal("65.00")
        );

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestSolapado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto de Agenda"));
    }

    @Test
    @DisplayName("GET /api/reservas/{id} debe retornar HTTP 200 OK con la reserva solicitada")
    void debeObtenerReservaExistenteYRetornar200() throws Exception {
        Reserva existente = new Reserva(
                null, 1L, 2L, 3L, 4L,
                LocalDateTime.of(2026, 9, 15, 12, 0),
                60, EstadoReserva.PENDIENTE, new BigDecimal("55.00")
        );
        Reserva guardada = reservaRepository.save(existente);

        mockMvc.perform(get("/api/reservas/{id}", guardada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guardada.getId()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("GET /api/reservas/{id} debe retornar HTTP 404 Not Found si no existe")
    void debeRetornar404CuandoReservaNoExiste() throws Exception {
        mockMvc.perform(get("/api/reservas/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}/confirmar debe actualizar el estado a CONFIRMADA y retornar HTTP 200")
    void debeConfirmarReservaYRetornar200() throws Exception {
        Reserva pendiente = new Reserva(
                null, 1L, 2L, 3L, 4L,
                LocalDateTime.of(2026, 9, 15, 14, 0),
                60, EstadoReserva.PENDIENTE, new BigDecimal("65.00")
        );
        Reserva guardada = reservaRepository.save(pendiente);

        mockMvc.perform(put("/api/reservas/{id}/confirmar", guardada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}/cancelar debe actualizar el estado a CANCELADA y retornar HTTP 200")
    void debeCancelarReservaYRetornar200() throws Exception {
        Reserva pendiente = new Reserva(
                null, 1L, 2L, 3L, 4L,
                LocalDateTime.of(2026, 9, 15, 16, 0),
                60, EstadoReserva.PENDIENTE, new BigDecimal("65.00")
        );
        Reserva guardada = reservaRepository.save(pendiente);

        mockMvc.perform(put("/api/reservas/{id}/cancelar", guardada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    @DisplayName("POST /api/reservas debe retornar HTTP 400 Bad Request si la fecha es en el pasado")
    void debeRetornar400CuandoFechaEsEnElPasado() throws Exception {
        CrearReservaRequest requestInvalido = new CrearReservaRequest(
                1L, 2L, 3L, 4L,
                LocalDateTime.now().minusDays(1),
                60, new BigDecimal("65.00")
        );

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Solicitud Inválida"));
    }
}
