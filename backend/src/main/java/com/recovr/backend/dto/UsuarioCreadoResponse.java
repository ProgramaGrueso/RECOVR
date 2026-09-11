package com.recovr.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta tras la creación exitosa de un usuario")
public record UsuarioCreadoResponse(
        @Schema(description = "Identificador único asignado", example = "2")
        Long id,

        @Schema(description = "Correo electrónico del usuario", example = "recepcion@recovr.com")
        String correo,

        @Schema(description = "Rol asignado", example = "RECEPCIONISTA")
        String rol
) {}
