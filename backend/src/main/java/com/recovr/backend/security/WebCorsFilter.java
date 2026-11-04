package com.recovr.backend.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * CORS con lista blanca configurable: {@code CORS_ALLOWED_ORIGINS} (separados por coma). Cada entrada es un origen
 * exacto ({@code https://recovr.example.com}) o un patrón explícito ({@code https://recovr-*.vercel.app}).
 * Un origen que no está en la lista no recibe {@code Access-Control-Allow-Origin} y su preflight se rechaza con 403.
 * Nunca se refleja un origen cualquiera.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebCorsFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(WebCorsFilter.class);

    private final CorsConfiguration permitidos = new CorsConfiguration();

    public WebCorsFilter(@Value("${recovr.cors.allowed-origins:}") String origenes) {
        List<String> lista = Arrays.stream(origenes.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (lista.contains("*")) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS no admite '*': indica los orígenes del frontend.");
        }
        permitidos.setAllowedOriginPatterns(lista);
        log.info("CORS: orígenes permitidos {}", lista);
    }

    /** ¿El origen está en la lista blanca? */
    boolean permitido(String origin) {
        return origin != null && permitidos.checkOrigin(origin) != null;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String origin = request.getHeader("Origin");
        boolean preflight = "OPTIONS".equalsIgnoreCase(request.getMethod())
                && request.getHeader("Access-Control-Request-Method") != null;

        if (origin != null) {
            response.addHeader("Vary", "Origin");
        }
        if (permitido(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
            response.setHeader("Access-Control-Expose-Headers", "X-Total-Count, X-Total-Pages");
            response.setHeader("Access-Control-Max-Age", "3600");
            // Chrome Private Network Access: una página https permitida puede llamar al backend en localhost
            if (request.getHeader("Access-Control-Request-Private-Network") != null) {
                response.setHeader("Access-Control-Allow-Private-Network", "true");
            }
        }
        if (preflight) {
            response.setStatus(permitido(origin) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(req, res);
    }
}
