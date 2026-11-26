package com.recovr.backend.config;

import com.recovr.backend.BackendApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj del negocio. Todo lo que dependa de "ahora" (plazos de pago, check-in, turnos, datos demo) lo toma de este
 * {@link Clock} en hora de Lima, sin depender de la zona horaria del servidor. En las pruebas se puede reemplazar
 * por un reloj fijo.
 */
@Configuration
public class RelojConfig {

    public static final ZoneId ZONA_NEGOCIO = ZoneId.of(BackendApplication.ZONA_NEGOCIO);

    @Bean
    public Clock relojNegocio() {
        return Clock.system(ZONA_NEGOCIO);
    }
}
