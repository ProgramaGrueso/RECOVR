package com.recovr.backend.repository;

import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    long countByRol(Rol rol);
}
