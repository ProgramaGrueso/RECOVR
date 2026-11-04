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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      @Value("${recovr.seguridad.exigir-jwt-secret:false}") boolean exigirSecreto) {
        this.expirationMs = expirationMs;
        if (secret == null || secret.isBlank()) {
            if (exigirSecreto) {
                // Con varias instancias cada una generaría su propia clave y los tokens no valdrían entre ellas.
                throw new IllegalStateException("JWT_SECRET es obligatoria en producción (Base64, 32 bytes o más).");
            }
            log.warn("JWT_SECRET no definida: se usa una clave temporal (los tokens se invalidan al reiniciar).");
            this.clave = Jwts.SIG.HS256.key().build();
        } else {
            this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
    }

    /** Emisor que se firma y se exige en cada token. */
    static final String EMISOR = "recovr-api";
    /** Claim con una huella de la contraseña vigente: si la clave cambia, los tokens anteriores dejan de valer. */
    static final String CLAIM_HUELLA = "hc";

    public String generarToken(UserDetails userDetails) {
        Date ahora = new Date();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(userDetails.getUsername())
                .claim(CLAIM_HUELLA, huella(userDetails.getPassword()))
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(clave())
                .compact();
    }

    public String extraerCorreo(String token) {
        return claims(token).getSubject();
    }

    /**
     * Firma válida (HS256 con la clave del servidor), emisor correcto, no vencido, del mismo usuario y emitido con la
     * contraseña que tiene ahora (cambiarla o restablecerla invalida todos sus tokens anteriores).
     */
    public boolean esValido(String token, UserDetails userDetails) {
        Claims c = claims(token);
        return userDetails.getUsername().equals(c.getSubject())
                && c.getExpiration().after(new Date())
                && huella(userDetails.getPassword()).equals(c.get(CLAIM_HUELLA, String.class));
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(clave()).requireIssuer(EMISOR).build().parseSignedClaims(token).getPayload();
    }

    /** Resumen corto (SHA-256) del hash BCrypt: no permite recuperar nada, pero cambia si la contraseña cambia. */
    static String huella(String hashClave) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(String.valueOf(hashClave).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private SecretKey clave() {
        return clave;
    }
}
