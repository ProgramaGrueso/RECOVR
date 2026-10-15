package com.recovr.backend.service;

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

/**
 * Mantiene los estados al día sin intervención humana. Se ejecuta al arrancar y cada minuto
 * (planificador desactivable con recovr.lifecycle.enabled=false). Aplica sobre las reservas activas cuyo inicio ya pasó:
 * <ol>
 *   <li><b>PENDIENTE</b> que no se confirmó a los {@value ReglasReserva#TOLERANCIA_CONFIRMACION_MIN} min de su inicio → CANCELADA (expirada).</li>
 *   <li><b>CONFIRMADA con check-in</b> cuya sesión ya terminó → COMPLETADA.</li>
 *   <li><b>CONFIRMADA sin check-in</b> a los {@value ReglasReserva#TOLERANCIA_NO_SHOW_MIN} min de su inicio → CANCELADA (no asistió).</li>
 * </ol>
 * Cada cambio deja el motivo en {@code observacion}. Es idempotente: ejecutarlo dos veces seguidas no cambia nada más.
 */
@Service
public class ReservaCicloVidaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaCicloVidaService.class);

    private final ReservaRepository reservaRepository;

    public ReservaCicloVidaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 60000)
    public void ejecutarPeriodicamente() {
        int cambios = actualizarEstados(LocalDateTime.now());
        if (cambios > 0) {
            log.info("Ciclo de vida de reservas: {} reserva(s) actualizadas automáticamente.", cambios);
        }
    }

    /** @return cuántas reservas cambiaron de estado. */
    @Transactional
    public int actualizarEstados(LocalDateTime ahora) {
        List<Reserva> candidatas = reservaRepository.buscarActivasIniciadasAntesDe(ahora);
        int cambios = 0;
        for (Reserva r : candidatas) {
            if (r.getEstado() == EstadoReserva.PENDIENTE) {
                if (!ahora.isBefore(r.getFechaHora().plusMinutes(ReglasReserva.TOLERANCIA_CONFIRMACION_MIN))) {
                    r.setEstado(EstadoReserva.CANCELADA);
                    r.setObservacion("Expirada automáticamente: no se confirmó el pago antes del inicio de la sesión.");
                    cambios++;
                }
            } else if (r.getCheckIn() != null) {
                if (!ahora.isBefore(ReglasReserva.finSesion(r))) {
                    r.setEstado(EstadoReserva.COMPLETADA);
                    r.setObservacion("Completada automáticamente al terminar la sesión.");
                    cambios++;
                }
            } else if (!ahora.isBefore(r.getFechaHora().plusMinutes(ReglasReserva.TOLERANCIA_NO_SHOW_MIN))) {
                r.setEstado(EstadoReserva.CANCELADA);
                r.setObservacion("No asistió: no hubo check-in dentro de la tolerancia.");
                cambios++;
            }
        }
        if (cambios > 0) {
            reservaRepository.saveAll(candidatas);
        }
        return cambios;
    }
}
