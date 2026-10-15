package com.recovr.backend.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebCorsFilter implements Filter {

    private static final java.util.regex.Pattern ORIGEN_PERMITIDO = java.util.regex.Pattern.compile(
            "^https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?$|^https://[a-z0-9-]+(\\.[a-z0-9-]+)*\\.vercel\\.app$");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader("Origin");
        // Solo orígenes conocidos (la app en desarrollo y los despliegues de Vercel); nunca se refleja un origen cualquiera.
        if (origin != null && ORIGEN_PERMITIDO.matcher(origin).matches()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Max-Age", "3600");

        // Soporte para Chrome Private Network Access (permite que páginas https externas llamen a localhost)
        if (request.getHeader("Access-Control-Request-Private-Network") != null) {
            response.setHeader("Access-Control-Allow-Private-Network", "true");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }
}
