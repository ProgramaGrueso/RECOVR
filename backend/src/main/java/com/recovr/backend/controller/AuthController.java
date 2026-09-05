package com.recovr.backend.controller;

import com.recovr.backend.dto.AuthResponse;
import com.recovr.backend.dto.CrearUsuarioRequest;
import com.recovr.backend.dto.LoginRequest;
import com.recovr.backend.dto.RegistroRequest;
import com.recovr.backend.dto.UsuarioCreadoResponse;
import com.recovr.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registroCliente(@Valid @RequestBody RegistroRequest request) {
        return authService.registrarCliente(request);
    }

    @PostMapping("/registro-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registroPrimerAdmin(@Valid @RequestBody RegistroRequest request) {
        return authService.registrarPrimerAdmin(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCreadoResponse crearUsuarioPersonal(@Valid @RequestBody CrearUsuarioRequest request) {
        return authService.crearUsuarioPersonal(request);
    }
}
