package com.recovr.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey clave;
    private final long expirationMs;

    /**
     * La clave sale de JWT_SECRET (Base64, mínimo 32 bytes). Si no está definida se genera una clave
     * aleatoria solo para esta ejecución: los tokens dejan de valer al reiniciar, nunca hay un secreto
     * por defecto versionado en el repositorio.
     */
    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.expirationMs = expirationMs;
        if (secret.isBlank()) {
            log.warn("JWT_SECRET no definida: se usa una clave temporal (los tokens se invalidan al reiniciar).");
            this.clave = Jwts.SIG.HS256.key().build();
        } else {
            this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
    }

    public String generarToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(clave())
                .compact();
    }

    public String extraerCorreo(String token) {
        return claims(token).getSubject();
    }

    public boolean esValido(String token, UserDetails userDetails) {
        return userDetails.getUsername().equals(extraerCorreo(token))
                && claims(token).getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(clave()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey clave() {
        return clave;
    }
}
