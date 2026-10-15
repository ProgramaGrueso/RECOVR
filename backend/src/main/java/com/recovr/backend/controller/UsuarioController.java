package com.recovr.backend.controller;

import com.recovr.backend.dto.UsuarioDtos.*;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios y accesos", description = "Alta, desactivación y restablecimiento de claves del personal. Solo ADMIN.")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar usuarios", description = "Todos los accesos; con ?rol=ADMIN|RECEPCIONISTA|ESPECIALISTA|CLIENTE se filtra.")
    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Rol rol) {
        return usuarioService.listar(rol);
    }

    @Operation(summary = "Crear acceso de personal",
            description = "ADMIN, RECEPCIONISTA o ESPECIALISTA (esta última vinculada a una terapeuta). Devuelve una clave temporal que se muestra una sola vez.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioNuevoResponse crear(@Valid @RequestBody NuevoUsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @Operation(summary = "Activar o desactivar un acceso", description = "No permite desactivarse a sí mismo ni al último ADMIN activo.")
    @PatchMapping("/{id}/activo")
    public UsuarioResponse cambiarActivo(@PathVariable Long id, @RequestParam boolean valor, Authentication auth) {
        return usuarioService.cambiarActivo(id, valor, auth.getName());
    }

    @Operation(summary = "Restablecer la clave", description = "Genera una clave temporal nueva (se muestra una sola vez) y obliga a cambiarla al ingresar.")
    @PostMapping("/{id}/restablecer-clave")
    public UsuarioNuevoResponse restablecerClave(@PathVariable Long id) {
        return usuarioService.restablecerClave(id);
    }
}
