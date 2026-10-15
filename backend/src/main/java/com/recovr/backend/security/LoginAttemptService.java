package com.recovr.backend.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra adivinar contraseñas: tras {@value #MAX_FALLOS} intentos fallidos seguidos de un mismo correo desde
 * una misma IP, ese par queda bloqueado {@code BLOQUEO} aunque luego envíe la contraseña correcta. Un inicio de sesión
 * correcto reinicia el contador. Es en memoria: se vacía al reiniciar el backend.
 */
@Component
public class LoginAttemptService {

    static final int MAX_FALLOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(5);

    private record Intento(int fallos, Instant bloqueadoHasta) {}

    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();

    private static String clave(String correo, String ip) {
        return (correo == null ? "" : correo.trim().toLowerCase()) + "|" + ip;
    }

    public boolean estaBloqueado(String correo, String ip, Instant ahora) {
        Intento i = intentos.get(clave(correo, ip));
        return i != null && i.bloqueadoHasta() != null && ahora.isBefore(i.bloqueadoHasta());
    }

    public void registrarFallo(String correo, String ip, Instant ahora) {
        intentos.merge(clave(correo, ip), new Intento(1, null), (viejo, nuevo) -> {
            if (viejo.bloqueadoHasta() != null && !ahora.isBefore(viejo.bloqueadoHasta())) {
                return new Intento(1, null); // el bloqueo anterior ya venció
            }
            int fallos = viejo.fallos() + 1;
            return new Intento(fallos, fallos >= MAX_FALLOS ? ahora.plus(BLOQUEO) : null);
        });
    }

    public void registrarExito(String correo, String ip) {
        intentos.remove(clave(correo, ip));
    }
}
