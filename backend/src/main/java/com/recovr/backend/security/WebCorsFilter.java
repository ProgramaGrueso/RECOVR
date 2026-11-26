package com.recovr.backend.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebCorsFilter implements Filter {

    private static final java.util.regex.Pattern ORIGEN_PERMITIDO = java.util.regex.Pattern.compile(
            "^https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?$"
                    + "|^https?://(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}"
                    + "|172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3})(:\\d+)?$"
                    + "|^https://[a-z0-9-]+(\\.[a-z0-9-]+)*\\.vercel\\.app$"
                    + "|^https://[a-z0-9-]+(\\.[a-z0-9-]+)*\\.trycloudflare\\.com$");

    /** Orígenes adicionales separados por coma en app.cors.origenes-extra (o CORS_ORIGENES_EXTRA). */
    private final Set<String> origenesExtra;

    public WebCorsFilter(@Value("${app.cors.origenes-extra:}") String extra) {
        this.origenesExtra = new LinkedHashSet<>();
        if (extra != null && !extra.isBlank()) {
            Arrays.stream(extra.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(WebCorsFilter::normalizar)
                    .forEach(origenesExtra::add);
        }
    }

    private static String normalizar(String origen) {
        String limpio = origen.toLowerCase(Locale.ROOT);
        while (limpio.endsWith("/")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }
        return limpio;
    }

    private boolean permitido(String origin) {
        return ORIGEN_PERMITIDO.matcher(origin).matches()
                || origenesExtra.contains(normalizar(origin));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader("Origin");
        // Solo orígenes conocidos (desarrollo local, red privada, Vercel, túneles Cloudflare y los extra configurados);
        // nunca se refleja un origen cualquiera.
        if (origin != null && permitido(origin)) {
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
