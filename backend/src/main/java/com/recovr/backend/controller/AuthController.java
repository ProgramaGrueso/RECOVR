package com.recovr.backend.controller;

import com.recovr.backend.dto.AuthResponse;
import com.recovr.backend.dto.PerfilResponse;
import com.recovr.backend.dto.UsuarioDtos.CambiarClaveRequest;
import com.recovr.backend.dto.LoginRequest;
import com.recovr.backend.dto.RegistroRequest;
import com.recovr.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints de autenticación, registro y generación de tokens JWT")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Registrar nuevo cliente",
            description = "Crea un usuario con rol CLIENTE y su ficha de cliente asociada. Devuelve el token JWT de sesión."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente y sesión iniciada",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o correo ya registrado", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registroCliente(@Valid @RequestBody RegistroRequest request) {
        return authService.registrarCliente(request);
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica credenciales de cualquier usuario (ADMIN, RECEPCIONISTA, ESPECIALISTA, CLIENTE) y retorna el Bearer Token JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación satisfactoria",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Formato de credenciales inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @Operation(summary = "Perfil de la sesión actual", description = "Devuelve correo, rol, nombre y vínculos (cliente o terapeuta) del token enviado. Sirve para validar la sesión.")
    @GetMapping("/me")
    public PerfilResponse me(org.springframework.security.core.Authentication authentication) {
        return authService.perfil(authentication.getName());
    }

    @Operation(summary = "Cambiar mi contraseña", description = "Exige la contraseña actual. Obligatorio en el primer ingreso con una clave temporal.")
    @PostMapping("/cambiar-clave")
    public void cambiarClave(@Valid @RequestBody CambiarClaveRequest request, org.springframework.security.core.Authentication authentication,
                             jakarta.servlet.http.HttpServletRequest http) {
        authService.cambiarClave(authentication.getName(), request, http.getRemoteAddr());
    }
}
