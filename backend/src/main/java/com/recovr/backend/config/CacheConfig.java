package com.recovr.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Caché en memoria (Caffeine) para el catálogo que se lee en cada pantalla y casi nunca cambia: servicios, cabinas y
 * terapeutas activas. Cada escritura del catálogo vacía su caché en esta instancia; con varias instancias el resto
 * se entera al vencer la entrada (CACHE_TTL, 60 s por defecto). Las reservas NUNCA se cachean: la agenda debe ser exacta.
 * Configuración: spring.cache.* en application.properties.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
