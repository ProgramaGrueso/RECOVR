package com.recovr.backend.service;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UsuarioActualServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @InjectMocks private UsuarioActualService usuarioActual;

    @Test
    @DisplayName("Del correo del token llega a la ficha de cliente")
    void clienteDelUsuario() {
        Usuario u = new Usuario();
        u.setId(7L);
        Cliente c = new Cliente();
        c.setId(42L);
        given(usuarioRepository.findByCorreo("ana@example.com")).willReturn(Optional.of(u));
        given(clienteRepository.findByUsuarioId(7L)).willReturn(Optional.of(c));

        assertThat(usuarioActual.clienteId("ana@example.com")).isEqualTo(42L);
    }

    @Test
    @DisplayName("Usuario inexistente: 404; cuenta sin terapeuta vinculada: 400 con el mensaje indicado")
    void errores() {
        given(usuarioRepository.findByCorreo("nadie@example.com")).willReturn(Optional.empty());
        assertThatThrownBy(() -> usuarioActual.cliente("nadie@example.com")).isInstanceOf(RecursoNoEncontradoException.class);

        Usuario u = new Usuario();
        u.setId(8L);
        given(usuarioRepository.findByCorreo("recep@example.com")).willReturn(Optional.of(u));
        given(empleadoRepository.findByUsuarioId(8L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> usuarioActual.empleado("recep@example.com"))
                .isInstanceOf(IllegalStateException.class).hasMessage(UsuarioActualService.SIN_TERAPEUTA);
    }
}
