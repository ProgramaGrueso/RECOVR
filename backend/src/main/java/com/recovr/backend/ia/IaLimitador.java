package com.recovr.backend.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Límites en memoria del chat con IA, para que nadie gaste el saldo de la API:
 * por minuto y por día por IP, y un tope diario global que pausa el chat entero.
 */
@Component
public class IaLimitador {

    public enum Veredicto { OK, DEMASIADO_RAPIDO, LIMITE_DIARIO_IP, LIMITE_DIARIO_GLOBAL }

    private static final long VENTANA_MS = 60_000L;
    private static final int MAX_IPS_EN_MEMORIA = 10_000;

    @Value("${recovr.ia.max-por-minuto-ip:10}")
    private int maxPorMinutoIp;

    @Value("${recovr.ia.max-por-dia-ip:100}")
    private int maxPorDiaIp;

    @Value("${recovr.ia.max-por-dia-global:1000}")
    private int maxPorDiaGlobal;

    private final Map<String, ConcurrentLinkedQueue<Long>> porMinuto = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> porDia = new ConcurrentHashMap<>();
    private final AtomicInteger globalHoy = new AtomicInteger();
    private volatile LocalDate dia = LocalDate.now();

    public Veredicto intentar(String ip) {
        reiniciarSiCambioElDia();

        if (globalHoy.get() >= maxPorDiaGlobal) return Veredicto.LIMITE_DIARIO_GLOBAL;

        AtomicInteger hoyIp = porDia.computeIfAbsent(ip, k -> new AtomicInteger());
        if (hoyIp.get() >= maxPorDiaIp) return Veredicto.LIMITE_DIARIO_IP;

        long ahora = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> marcas = porMinuto.computeIfAbsent(ip, k -> new ConcurrentLinkedQueue<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peek() > VENTANA_MS) marcas.poll();
            if (marcas.size() >= maxPorMinutoIp) return Veredicto.DEMASIADO_RAPIDO;
            marcas.add(ahora);
        }

        hoyIp.incrementAndGet();
        globalHoy.incrementAndGet();
        return Veredicto.OK;
    }

    private synchronized void reiniciarSiCambioElDia() {
        LocalDate hoy = LocalDate.now();
        if (!hoy.equals(dia)) {
            dia = hoy;
            porDia.clear();
            globalHoy.set(0);
        }
        if (porMinuto.size() > MAX_IPS_EN_MEMORIA) {
            long ahora = System.currentTimeMillis();
            porMinuto.values().removeIf(q -> q.isEmpty() || ahora - q.peek() > VENTANA_MS);
        }
    }
}
