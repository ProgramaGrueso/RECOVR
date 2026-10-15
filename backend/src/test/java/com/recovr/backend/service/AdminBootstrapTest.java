package com.recovr.backend.service;

import com.recovr.backend.config.AdminBootstrap;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Crea el primer administrador desde la configuración, con la contraseña cifrada y cambio obligatorio")
    void creaElPrimerAdmin() {
        when(repo.countByRol(Rol.ADMIN)).thenReturn(0L);
        new AdminBootstrap(repo, encoder, " Dueño@Recovr.pe ", "Inicial12345", "Dueño del spa").run();
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(cap.capture());
        Usuario u = cap.getValue();
        assertThat(u.getCorreo()).isEqualTo("dueño@recovr.pe");
        assertThat(u.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(u.isDebeCambiarClave()).isTrue();
        assertThat(u.getPassword()).isNotEqualTo("Inicial12345");
        assertThat(encoder.matches("Inicial12345", u.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Si ya existe un administrador no hace nada, y sin configuración tampoco crea nada")
    void noPisaNiInventa() {
        when(repo.countByRol(Rol.ADMIN)).thenReturn(1L);
        new AdminBootstrap(repo, encoder, "otro@recovr.pe", "Inicial12345", "X").run();
        when(repo.countByRol(Rol.ADMIN)).thenReturn(0L);
        new AdminBootstrap(repo, encoder, "", "", "X").run();
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Una contraseña inicial débil detiene el arranque en vez de crear un admin inseguro")
    void rechazaClaveDebil() {
        when(repo.countByRol(Rol.ADMIN)).thenReturn(0L);
        assertThatThrownBy(() -> new AdminBootstrap(repo, encoder, "a@b.pe", "123456", "X").run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("política");
        verify(repo, never()).save(any());
    }
}
