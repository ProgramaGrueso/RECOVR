package com.recovr.backend.controller;

import com.recovr.backend.dto.AuthResponse;
import com.recovr.backend.dto.CrearUsuarioRequest;
import com.recovr.backend.dto.LoginRequest;
import com.recovr.backend.dto.RegistroRequest;
import com.recovr.backend.dto.UsuarioCreadoResponse;
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
            summary = "Registrar el primer administrador",
            description = "Permite crear el usuario inicial con rol ADMIN si no existe ninguno previamente en el sistema."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Administrador registrado exitosamente y sesión iniciada",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ya existe un usuario administrador registrado o datos inválidos", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/registro-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registroPrimerAdmin(@Valid @RequestBody RegistroRequest request) {
        return authService.registrarPrimerAdmin(request);
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
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(
            summary = "Crear usuario de personal interno",
            description = "Permite a un administrador registrar cuentas para personal operativo (RECEPCIONISTA o ESPECIALISTA). Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario de personal creado satisfactoriamente",
                    content = @Content(schema = @Schema(implementation = UsuarioCreadoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o correo ya en uso", content = @Content),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content)
    })
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCreadoResponse crearUsuarioPersonal(@Valid @RequestBody CrearUsuarioRequest request) {
        return authService.crearUsuarioPersonal(request);
    }
}
