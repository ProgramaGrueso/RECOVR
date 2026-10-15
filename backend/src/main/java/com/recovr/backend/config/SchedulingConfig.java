package com.recovr.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa el planificador que actualiza los estados de las reservas (se apaga con recovr.lifecycle.enabled=false). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "recovr.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
