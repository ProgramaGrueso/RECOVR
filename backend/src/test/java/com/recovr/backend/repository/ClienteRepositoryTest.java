package com.recovr.backend.repository;

import com.recovr.backend.entity.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    @DisplayName("Debe guardar y recuperar cliente en H2 in-memory DB")
    void debeGuardarYBuscarCliente() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Ana Martinez");
        cliente.setCorreo("ana@example.com");
        cliente.setTelefono("987654321");

        Cliente guardado = clienteRepository.save(cliente);

        assertThat(guardado.getId()).isNotNull();

        Optional<Cliente> encontrado = clienteRepository.findById(guardado.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("Ana Martinez");
    }
}
