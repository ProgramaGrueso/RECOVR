package com.recovr.backend.service;

import java.time.Clock;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mantiene los estados al día sin intervención humana. Se ejecuta al arrancar y cada minuto
 * (planificador desactivable con recovr.lifecycle.enabled=false). Aplica sobre las reservas activas cuyo inicio ya pasó:
 * <ol>
 *   <li><b>PENDIENTE</b> que no se pagó completa a los {@value ReglasReserva#HOLD_PAGO_MIN} min de crearse → CANCELADA (expirada,
 *       el cupo se libera). Las creadas antes de registrarse la fecha de creación expiran a los
 *       {@value ReglasReserva#TOLERANCIA_CONFIRMACION_MIN} min de su inicio.</li>
 *   <li><b>CONFIRMADA con check-in</b> cuya sesión ya terminó → COMPLETADA.</li>
 *   <li><b>CONFIRMADA sin check-in</b> a los {@value ReglasReserva#TOLERANCIA_NO_SHOW_MIN} min de su inicio → CANCELADA (no asistió).</li>
 * </ol>
 * Cada cambio deja el motivo en {@code observacion}. Es idempotente: ejecutarlo dos veces seguidas no cambia nada más.
 */
@Service
public class ReservaCicloVidaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaCicloVidaService.class);

    private final ReservaRepository reservaRepository;
    private volatile LocalDateTime ultimaEjecucion;
    private volatile int ultimosCambios;
    private final AtomicLong totalCambios = new AtomicLong();

    /** Reloj del negocio (hora de Lima): nunca la zona horaria del servidor. */
    private final Clock reloj;

    public ReservaCicloVidaService(ReservaRepository reservaRepository, Clock reloj) {
        this.reloj = reloj;
        this.reservaRepository = reservaRepository;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 60000)
    public void ejecutarPeriodicamente() {
        int cambios = actualizarEstados(LocalDateTime.now(reloj));
        if (cambios > 0) {
            log.info("Ciclo de vida de reservas: {} reserva(s) actualizadas automáticamente.", cambios);
        }
    }

    public LocalDateTime getUltimaEjecucion() { return ultimaEjecucion; }
    public int getUltimosCambios() { return ultimosCambios; }
    public long getTotalCambios() { return totalCambios.get(); }

    /**
     * Libera el cupo de las reservas PENDIENTES cuyo plazo de pago ya venció. Lo usa el planificador y también
     * {@link ReservaService} antes de crear o mover una reserva, para no esperar al siguiente minuto.
     *
     * @return cuántas reservas expiraron.
     */
    @Transactional
    public int expirarPlazosVencidos(LocalDateTime ahora) {
        List<Reserva> sinPagar = reservaRepository.buscarPendientesCreadasHasta(ahora.minusMinutes(ReglasReserva.HOLD_PAGO_MIN));
        sinPagar.forEach(r -> ReglasReserva.cancelar(r, ahora, ReglasReserva.EXPIRADA_SIN_PAGO));
        reservaRepository.saveAll(sinPagar);
        return sinPagar.size();
    }

    /** @return cuántas reservas cambiaron de estado. */
    @Transactional
    public int actualizarEstados(LocalDateTime ahora) {
        int cambios = expirarPlazosVencidos(ahora);
        List<Reserva> candidatas = reservaRepository.buscarActivasIniciadasAntesDe(ahora);
        int cambiosInicio = 0;
        for (Reserva r : candidatas) {
            if (aplicarCambioAutomatico(r, ahora)) {
                cambiosInicio++;
            }
        }
        if (cambiosInicio > 0) {
            reservaRepository.saveAll(candidatas);
        }
        cambios += cambiosInicio;
        ultimaEjecucion = ahora;
        ultimosCambios = cambios;
        totalCambios.addAndGet(cambios);
        return cambios;
    }

    /** Regla para una reserva activa cuyo inicio ya pasó; devuelve true si cambió de estado. */
    private static boolean aplicarCambioAutomatico(Reserva r, LocalDateTime ahora) {
        if (r.getEstado() == EstadoReserva.PENDIENTE) {
            if (!ahora.isBefore(r.getFechaHora().plusMinutes(ReglasReserva.TOLERANCIA_CONFIRMACION_MIN))) {
                ReglasReserva.cancelar(r, ahora, ReglasReserva.EXPIRADA_SIN_PAGO);
                return true;
            }
            return false;
        }
        if (r.getCheckIn() != null) {
            if (!ahora.isBefore(ReglasReserva.finSesion(r))) {
                r.setEstado(EstadoReserva.COMPLETADA);
                r.setObservacion("Completada automáticamente al terminar la sesión.");
                return true;
            }
            return false;
        }
        if (!ahora.isBefore(r.getFechaHora().plusMinutes(ReglasReserva.TOLERANCIA_NO_SHOW_MIN))) {
            ReglasReserva.cancelar(r, ahora, ReglasReserva.NO_ASISTIO);
            return true;
        }
        return false;
    }
}
