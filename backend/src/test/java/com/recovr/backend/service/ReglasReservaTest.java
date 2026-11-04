package com.recovr.backend.service;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Servicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Reglas puras del ciclo de vida de una reserva (sin Spring ni base de datos). */
class ReglasReservaTest {

    private static final LocalDateTime DIA = LocalDateTime.of(2026, 11, 20, 18, 0);

    @Test
    @DisplayName("El bloque ocupa duración + limpieza; sin datos se asume 60 min y 0 de limpieza")
    void finDeBloque() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, 90, 20);
        assertThat(ReglasReserva.finSesion(r)).isEqualTo(DIA.plusMinutes(90));
        assertThat(ReglasReserva.finBloque(r)).isEqualTo(DIA.plusMinutes(110));
        assertThat(ReglasReserva.finBloque(DIA, new Servicio())).isEqualTo(DIA.plusMinutes(60));
    }

    @Test
    @DisplayName("Horario de atención: la sesión entera entre 12:00 y 23:00 y el mismo día")
    void horarioDeAtencion() {
        assertThat(ReglasReserva.dentroDeHorario(DIA.withHour(12), DIA.withHour(13))).isTrue();
        assertThat(ReglasReserva.dentroDeHorario(DIA.withHour(22), DIA.withHour(23))).isTrue();
        assertThat(ReglasReserva.dentroDeHorario(DIA.withHour(11).withMinute(59), DIA.withHour(13))).isFalse();
        assertThat(ReglasReserva.dentroDeHorario(DIA.withHour(22), DIA.withHour(23).withMinute(1))).isFalse();
        assertThat(ReglasReserva.dentroDeHorario(DIA.withHour(22), DIA.plusDays(1).withHour(0))).isFalse();
    }

    @Test
    @DisplayName("El plazo de pago vence a los 15 min de crearse, solo para PENDIENTE con fecha de creación")
    void plazoDePago() {
        Reserva r = reserva(EstadoReserva.PENDIENTE, 60, 15);
        r.setCreadaEn(DIA.minusHours(2));
        assertThat(ReglasReserva.holdVencido(r, DIA.minusHours(2).plusMinutes(14))).isFalse();
        assertThat(ReglasReserva.holdVencido(r, DIA.minusHours(2).plusMinutes(15))).isTrue();
        r.setCreadaEn(null);
        assertThat(ReglasReserva.holdVencido(r, DIA)).isFalse();
        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA, 60, 15);
        confirmada.setCreadaEn(DIA.minusDays(1));
        assertThat(ReglasReserva.holdVencido(confirmada, DIA)).isFalse();
    }

    @Test
    @DisplayName("Estado operativo: PENDIENTE es EN_ESPERA y CONFIRMADA con check-in es EN_SESION")
    void estadoOperativo() {
        assertThat(ReglasReserva.estadoOperativo(reserva(EstadoReserva.PENDIENTE, 60, 0))).isEqualTo(ReglasReserva.EN_ESPERA);
        Reserva r = reserva(EstadoReserva.CONFIRMADA, 60, 0);
        assertThat(ReglasReserva.estadoOperativo(r)).isEqualTo(ReglasReserva.CONFIRMADA);
        r.setCheckIn(DIA);
        assertThat(ReglasReserva.estadoOperativo(r)).isEqualTo(ReglasReserva.EN_SESION);
        assertThat(ReglasReserva.activa(r)).isTrue();
        assertThat(ReglasReserva.activa(reserva(EstadoReserva.COMPLETADA, 60, 0))).isFalse();
    }

    @Test
    @DisplayName("Cancelar deja estado, momento y motivo")
    void cancelar() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, 60, 15);
        ReglasReserva.cancelar(r, DIA, ReglasReserva.NO_ASISTIO);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(r.getCanceladaEn()).isEqualTo(DIA);
        assertThat(r.getObservacion()).isEqualTo(ReglasReserva.NO_ASISTIO);
    }

    private static Reserva reserva(EstadoReserva estado, int duracion, int limpieza) {
        Servicio s = new Servicio();
        s.setDuracionMinutos(duracion);
        s.setTiempoLimpiezaMinutos(limpieza);
        Reserva r = new Reserva();
        r.setServicio(s);
        r.setFechaHora(DIA);
        r.setEstado(estado);
        return r;
    }
}
