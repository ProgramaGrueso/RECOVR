package com.recovr.backend.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente mínimo de la API de DeepSeek (compatible con OpenAI: /chat/completions).
 * La clave viene del entorno (DEEPSEEK_API_KEY) y no se escribe en ningún log.
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    /** Tope de tokens por respuesta; con 500 caracteres alcanza de sobra. */
    private static final int MAX_TOKENS = 300;

    private final ObjectMapper mapper;
    private final RestClient http;
    private final String apiKey;
    private final String modelo;

    public DeepSeekClient(
            ObjectMapper mapper,
            @Value("${recovr.ia.api-key:}") String apiKey,
            @Value("${recovr.ia.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${recovr.ia.modelo:deepseek-chat}") String modelo
    ) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(30_000);
        this.mapper = mapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo;
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    public boolean configurado() {
        return !apiKey.isBlank();
    }

    /** Una ronda de conversación; devuelve el mensaje del asistente (texto o tool_calls). */
    public JsonNode completar(ArrayNode mensajes, ArrayNode herramientas) {
        ObjectNode cuerpo = mapper.createObjectNode();
        cuerpo.put("model", modelo);
        cuerpo.put("max_tokens", MAX_TOKENS);
        cuerpo.put("temperature", 0.3);
        cuerpo.set("messages", mensajes);
        cuerpo.set("tools", herramientas);
        try {
            JsonNode respuesta = http.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo.toString())
                    .retrieve()
                    .body(JsonNode.class);
            if (respuesta == null) throw new IaNoDisponibleException("El asistente no está disponible por ahora.");
            return respuesta.path("choices").path(0).path("message");
        } catch (RestClientException e) {
            log.warn("DeepSeek no respondió: {}", e.getClass().getSimpleName());
            throw new IaNoDisponibleException("El asistente no está disponible por ahora.");
        }
    }
}
