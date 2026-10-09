package com.recovr.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Autenticado pero sin el rol requerido: 403 (antes terminaba en 401 vía /error)
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":403,\"error\":\"Acceso denegado\","
                                    + "\"message\":\"No tiene permisos para acceder a este recurso\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // /error debe ser público para que los errores 400/404/409 no se conviertan en 401
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/registro", "/api/auth/registro-admin", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/servicios/**")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA", "ESPECIALISTA", "CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/mias").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/cliente/**")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/confirmar-y-pagar")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/reservas")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservas/**")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/**")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/**")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA")
                        // Motor de reservas persistente (/api/db/reservas): reglas por rol
                        .requestMatchers(HttpMethod.GET, "/api/db/reservas/mias").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/db/reservas/agenda").hasRole("ESPECIALISTA")
                        .requestMatchers(HttpMethod.GET, "/api/db/reservas/cliente/**")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/db/reservas/*/confirmar-y-pagar")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/db/reservas")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA", "CLIENTE")
                        .requestMatchers("/api/db/reservas", "/api/db/reservas/**")
                            .hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/api/clientes/**", "/api/empleados/**", "/api/salas/**", "/api/pagos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/servicios/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
