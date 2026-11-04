package com.recovr.backend.exception;

import com.recovr.backend.config.RelojConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formato único de los errores de la API: {@code {timestamp, status, error, message}}. Lo usan el
 * {@link ApiExceptionHandler} y los filtros de seguridad (que responden antes de llegar a un controlador).
 */
public final class CuerpoError {

    private static final ObjectMapper JSON = new ObjectMapper();

    private CuerpoError() {}

    public static Map<String, Object> de(HttpStatus status, String error, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(RelojConfig.ZONA_NEGOCIO).toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", mensaje);
        return body;
    }

    /** Escribe el error directamente en la respuesta (para filtros y manejadores de Spring Security). */
    public static void escribir(HttpServletResponse response, HttpStatus status, String error, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JSON.writeValue(response.getWriter(), de(status, error, mensaje));
    }
}
