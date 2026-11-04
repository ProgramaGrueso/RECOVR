package com.recovr.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra adivinar contraseñas: tras {@value #MAX_FALLOS} intentos fallidos seguidos de un mismo correo desde
 * una misma IP, ese par queda bloqueado {@code BLOQUEO} aunque luego envíe la contraseña correcta. Un inicio de sesión
 * correcto reinicia el contador. Además, una IP con {@code max-fallos-ip} fallos (20) en {@code VENTANA_IP} (contra
 * cualquier correo) queda frenada, y el registro de cuentas se limita a {@code max-registros-ip} (5) por hora y por IP.
 * Es en memoria y por instancia: se vacía al reiniciar (con varias réplicas, ver RESUMEN.md).
 */
@Component
public class LoginAttemptService {

    static final int MAX_FALLOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(5);
    static final Duration VENTANA_IP = Duration.ofMinutes(10);
    static final Duration VENTANA_REGISTRO = Duration.ofHours(1);

    /** Logins fallidos desde una misma IP (contra cualquier correo) admitidos en {@link #VENTANA_IP}. */
    private final int maxFallosIp;
    /** Cuentas nuevas que se pueden registrar desde una misma IP por hora. */
    private final int maxRegistrosIp;

    public LoginAttemptService(@Value("${recovr.seguridad.max-fallos-ip:20}") int maxFallosIp,
                               @Value("${recovr.seguridad.max-registros-ip:5}") int maxRegistrosIp) {
        this.maxFallosIp = maxFallosIp;
        this.maxRegistrosIp = maxRegistrosIp;
    }

    private record Intento(int fallos, Instant bloqueadoHasta) {}

    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> fallosPorIp = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> registrosPorIp = new ConcurrentHashMap<>();

    private static String clave(String correo, String ip) {
        return (correo == null ? "" : correo.trim().toLowerCase()) + "|" + ip;
    }

    public boolean estaBloqueado(String correo, String ip, Instant ahora) {
        Intento i = intentos.get(clave(correo, ip));
        boolean parBloqueado = i != null && i.bloqueadoHasta() != null && ahora.isBefore(i.bloqueadoHasta());
        return parBloqueado || enVentana(fallosPorIp, ip, VENTANA_IP, ahora) >= maxFallosIp;
    }

    /** ¿Puede esta IP registrar otra cuenta? Si puede, cuenta el registro. */
    public boolean permitirRegistro(String ip, Instant ahora) {
        Deque<Instant> marcas = registrosPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (marcas) {
            purgar(marcas, VENTANA_REGISTRO, ahora);
            if (marcas.size() >= maxRegistrosIp) {
                return false;
            }
            marcas.addLast(ahora);
            return true;
        }
    }

    private static int enVentana(Map<String, Deque<Instant>> mapa, String ip, Duration ventana, Instant ahora) {
        Deque<Instant> marcas = mapa.get(ip);
        if (marcas == null) {
            return 0;
        }
        synchronized (marcas) {
            purgar(marcas, ventana, ahora);
            return marcas.size();
        }
    }

    private static void purgar(Deque<Instant> marcas, Duration ventana, Instant ahora) {
        while (!marcas.isEmpty() && !marcas.peekFirst().isAfter(ahora.minus(ventana))) {
            marcas.pollFirst();
        }
    }

    public void registrarFallo(String correo, String ip, Instant ahora) {
        Deque<Instant> marcasIp = fallosPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (marcasIp) {
            marcasIp.addLast(ahora);
        }
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
