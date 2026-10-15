package com.recovr.backend.controller;

import com.recovr.backend.dto.PerfilTerapeutaDtos.*;
import com.recovr.backend.service.PerfilTerapeutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/mi-perfil")
@Tag(name = "Mi perfil (terapeuta)", description = "Cada terapeuta edita su presentación, su teléfono y su propia foto. Solo ESPECIALISTA.")
public class MiPerfilController {

    private final PerfilTerapeutaService perfilService;

    public MiPerfilController(PerfilTerapeutaService perfilService) {
        this.perfilService = perfilService;
    }

    @Operation(summary = "Ver mi perfil")
    @GetMapping
    public MiPerfilResponse ver(Authentication auth) {
        return perfilService.miPerfil(auth.getName());
    }

    @Operation(summary = "Actualizar mi teléfono y mi presentación")
    @PutMapping
    public MiPerfilResponse actualizar(@Valid @RequestBody ActualizarMiPerfilRequest request, Authentication auth) {
        return perfilService.actualizar(auth.getName(), request);
    }

    @Operation(summary = "Subir mi foto de perfil", description = "JPG o PNG de hasta 3 MB. Se muestra en el sitio público.")
    @PutMapping(path = "/foto", consumes = "multipart/form-data")
    public MiPerfilResponse subirFoto(@RequestParam("archivo") MultipartFile archivo, Authentication auth) throws IOException {
        return perfilService.subirFoto(auth.getName(), archivo.getBytes());
    }

    @Operation(summary = "Quitar mi foto de perfil")
    @DeleteMapping("/foto")
    public MiPerfilResponse quitarFoto(Authentication auth) {
        return perfilService.quitarFoto(auth.getName());
    }
}
