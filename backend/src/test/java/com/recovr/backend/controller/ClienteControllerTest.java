package com.recovr.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.service.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
public class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteService clienteService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/clientes debe retornar la lista de clientes")
    void debeRetornarListaDeClientes() throws Exception {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNombre("Carlos Perez");
        c.setCorreo("carlos@example.com");

        given(clienteService.listarTodos()).willReturn(List.of(c));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$[0].correo").value("carlos@example.com"));
    }

    @Test
    @DisplayName("POST /api/clientes debe crear y retornar cliente")
    void debeCrearClienteViaApi() throws Exception {
        Cliente c = new Cliente();
        c.setNombre("Maria Lopez");
        c.setCorreo("maria@example.com");

        Cliente guardado = new Cliente();
        guardado.setId(2L);
        guardado.setNombre("Maria Lopez");
        guardado.setCorreo("maria@example.com");

        given(clienteService.crear(any(Cliente.class))).willReturn(guardado);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nombre").value("Maria Lopez"));
    }
}
