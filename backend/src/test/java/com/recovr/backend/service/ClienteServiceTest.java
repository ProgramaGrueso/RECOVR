package com.recovr.backend.service;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente clienteEjemplo;

    @BeforeEach
    void setUp() {
        clienteEjemplo = new Cliente();
        clienteEjemplo.setId(1L);
        clienteEjemplo.setNombre("Juan Ganoza");
        clienteEjemplo.setCorreo("juanganoza@hotmail.com");
        clienteEjemplo.setTelefono("+51999888777");
        clienteEjemplo.setUsuarioId(101L);
    }

    @Test
    @DisplayName("Debe listar todos los clientes")
    void debeListarTodosLosClientes() {
        given(clienteRepository.findAll()).willReturn(List.of(clienteEjemplo));

        List<Cliente> resultado = clienteService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Juan Ganoza");
    }

    @Test
    @DisplayName("Debe buscar cliente por ID cuando existe")
    void debeBuscarClientePorId() {
        given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteEjemplo));

        Cliente cliente = clienteService.buscarPorId(1L);

        assertThat(cliente).isNotNull();
        assertThat(cliente.getCorreo()).isEqualTo("juanganoza@hotmail.com");
    }

    @Test
    @DisplayName("Debe lanzar excepcion al buscar cliente inexistente")
    void debeLanzarExcepcionAlBuscarIdInexistente() {
        given(clienteRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cliente no encontrado con id 99");
    }

    @Test
    @DisplayName("Debe crear un nuevo cliente")
    void debeCrearCliente() {
        given(clienteRepository.save(any(Cliente.class))).willReturn(clienteEjemplo);

        Cliente creado = clienteService.crear(clienteEjemplo);

        assertThat(creado).isNotNull();
        verify(clienteRepository).save(clienteEjemplo);
    }

    @Test
    @DisplayName("Debe eliminar cliente por ID")
    void debeEliminarCliente() {
        clienteService.eliminar(1L);
        verify(clienteRepository).deleteById(1L);
    }
}
