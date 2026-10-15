package com.recovr.backend.controller;

import com.recovr.backend.dto.PerfilTerapeutaDtos.TerapeutaPublica;
import com.recovr.backend.entity.FotoEmpleado;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.service.FotoService;
import com.recovr.backend.service.PerfilTerapeutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/** Datos que el sitio público muestra sin iniciar sesión: solo terapeutas activas, sin teléfono ni accesos. */
@RestController
@RequestMapping("/api/publico")
@Tag(name = "Público", description = "Sin autenticación. Solo expone nombre, especialidad, presentación y foto de terapeutas activas.")
public class PublicoController {

    private final PerfilTerapeutaService perfilService;
    private final FotoService fotoService;
    private final EmpleadoRepository empleadoRepository;

    public PublicoController(PerfilTerapeutaService perfilService, FotoService fotoService, EmpleadoRepository empleadoRepository) {
        this.perfilService = perfilService;
        this.fotoService = fotoService;
        this.empleadoRepository = empleadoRepository;
    }

    @Operation(summary = "Terapeutas del sitio")
    @SecurityRequirements
    @GetMapping("/terapeutas")
    public List<TerapeutaPublica> terapeutas() {
        return perfilService.publicas();
    }

    @Operation(summary = "Foto de una terapeuta activa")
    @SecurityRequirements
    @GetMapping("/terapeutas/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        Empleado e = empleadoRepository.findById(id).filter(Empleado::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Terapeuta", id));
        FotoEmpleado foto = fotoService.obtener(e.getId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.getTipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.getDatos());
    }
}
