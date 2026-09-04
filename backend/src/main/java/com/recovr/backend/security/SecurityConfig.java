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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
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
