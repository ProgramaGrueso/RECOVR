package com.recovr.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    @DisplayName("En producción sin JWT_SECRET no arranca (cada instancia tendría su propia clave)")
    void exigeSecretoEnProduccion() {
        assertThatThrownBy(() -> new JwtService("", 3_600_000, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET");
        assertThatCode(() -> new JwtService("", 3_600_000, false)).doesNotThrowAnyException();
    }
}
