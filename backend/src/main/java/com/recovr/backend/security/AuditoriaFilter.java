package com.recovr.backend.security;

import com.recovr.backend.service.AuditoriaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Audita toda operación que modifica datos (POST, PUT, PATCH, DELETE) y todo acceso rechazado (401 o 403) en /api.
 * Se coloca dentro de la cadena de Spring Security, después de identificar al usuario y antes de autorizar, para ver
 * también los intentos denegados. Registra método, ruta, resultado, usuario, rol e IP; jamás el cuerpo de la petición.
 * El inicio de sesión se audita aparte (con el correo) en AuthService.
 */
@Component
public class AuditoriaFilter extends OncePerRequestFilter {

    private final AuditoriaService auditoriaService;

    public AuditoriaFilter(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/auth/") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth != null && auth.isAuthenticated() ? auth.getName() : "anónimo";
        String rol = auth == null ? "-" : auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("-");
        try {
            chain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            boolean modifica = !"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod());
            boolean denegado = status == 401 || status == 403;
            if (modifica || denegado) {
                auditoriaService.registrar(usuario, rol, request.getMethod(), request.getRequestURI(), status,
                        request.getRemoteAddr(), denegado ? "ACCESO DENEGADO" : null);
            }
        }
    }
}
