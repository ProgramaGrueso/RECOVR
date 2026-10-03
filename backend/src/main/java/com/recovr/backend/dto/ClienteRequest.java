package com.recovr.backend.dto;

import com.recovr.backend.entity.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar un cliente desde administración")
public record ClienteRequest(
        @Schema(description = "Nombre completo", example = "Maria Lopez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String nombre,

        @Schema(description = "Correo electrónico", example = "maria@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String correo,

        @Schema(description = "Teléfono de contacto", example = "999888777")
        @Size(max = 20) String telefono
) {
    public Cliente aEntidad() {
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setCorreo(correo);
        cliente.setTelefono(telefono);
        return cliente;
    }
}
