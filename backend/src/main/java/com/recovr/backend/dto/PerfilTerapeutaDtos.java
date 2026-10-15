package com.recovr.backend.dto;

import jakarta.validation.constraints.Size;

/** Perfil de una terapeuta. Público y privado comparten la misma base: nombre, especialidad, presentación y foto. */
public final class PerfilTerapeutaDtos {

    private PerfilTerapeutaDtos() {}

    public record MiPerfilResponse(Long id, String nombre, String especialidad, String telefono, String bio, boolean tieneFoto) {}

    /** La terapeuta edita su contacto y su presentación; nombre y especialidad los define el administrador. */
    public record ActualizarMiPerfilRequest(@Size(max = 20) String telefono, @Size(max = 500) String bio) {}

    public record TerapeutaPublica(Long id, String nombre, String especialidad, String bio, String fotoUrl) {}
}
