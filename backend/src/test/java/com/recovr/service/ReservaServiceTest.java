package com.recovr.service;

import com.recovr.exception.ReservaConflictException;
import com.recovr.exception.ReservaNotFoundException;
import com.recovr.model.EstadoReserva;
import com.recovr.model.Reserva;
import com.recovr.model.Servicio;
import com.recovr.repository.InMemoryReservaRepository;
import com.recovr.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservaServiceTest {

    private ReservaService reservaService;
    private ReservaRepository reservaRepository;

    @BeforeEach
    void setUp() {
        reservaRepository = new InMemoryReservaRepository();
        reservaService = new ReservaService(reservaRepository);
    }

    @Test
    @DisplayName("Debe calcular la hora de finalización del bloque sumando duración del servicio y tiempo de limpieza")
    void debeCalcularHoraFinBloqueSumandoDuracionYLimpieza() {
        Servicio servicio = new Servicio(1L, "Descompresión Miofascial Profunda", 75, 15, new BigDecimal("65.00"));
        LocalDateTime inicio = LocalDateTime.of(2026, 9, 10, 10, 0);

        LocalDateTime finBloque = reservaService.calcularFinBloque(inicio, servicio);

        assertNotNull(finBloque);
        assertEquals(LocalDateTime.of(2026, 9, 10, 11, 30), finBloque);
    }

    @Test
    @DisplayName("Debe crear una reserva con estado PENDIENTE cuando los recursos están disponibles")
    void debeCrearReservaConEstadoPendienteCuandoNoHayConflicto() {
        Reserva nuevaReserva = new Reserva(
                null, 10L, 1L, 2L, 1L,
                LocalDateTime.of(2026, 9, 10, 14, 0),
                60, null, new BigDecimal("55.00")
        );

        Reserva creada = reservaService.crearReserva(nuevaReserva);

        assertNotNull(creada.getId(), "La reserva creada debe tener un identificador asignado");
        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
        assertEquals(10L, creada.getClienteId());
    }

    @Test
    @DisplayName("Debe lanzar ReservaConflictException si el especialista ya tiene una reserva en ese horario")
    void debeLanzarExcepcionCuandoEspecialistaTieneSolapamiento() {
        // Reserva existente: Especialista 1 de 15:00 a 16:30 (90 min)
        Reserva existente = new Reserva(
                null, 11L, 1L, 1L, 1L,
                LocalDateTime.of(2026, 9, 10, 15, 0),
                90, EstadoReserva.CONFIRMADA, new BigDecimal("65.00")
        );
        reservaRepository.save(existente);

        // Intento de nueva reserva con el mismo especialista 1 solapada (15:30 a 16:30)
        Reserva solapada = new Reserva(
                null, 12L, 1L, 2L, 2L,
                LocalDateTime.of(2026, 9, 10, 15, 30),
                60, null, new BigDecimal("55.00")
        );

        ReservaConflictException ex = assertThrows(ReservaConflictException.class, () ->
                reservaService.crearReserva(solapada)
        );
        assertTrue(ex.getMessage().contains("especialista seleccionado no se encuentra disponible"));
    }

    @Test
    @DisplayName("Debe lanzar ReservaConflictException si la sala/suite de tratamiento ya se encuentra ocupada")
    void debeLanzarExcepcionCuandoSalaTieneSolapamiento() {
        // Reserva existente en Sala 3 de 16:00 a 17:15 (75 min)
        Reserva existente = new Reserva(
                null, 11L, 2L, 1L, 3L,
                LocalDateTime.of(2026, 9, 10, 16, 0),
                75, EstadoReserva.PENDIENTE, new BigDecimal("65.00")
        );
        reservaRepository.save(existente);

        // Intento de nueva reserva en la misma Sala 3 solapada (16:30 a 17:30) con otro empleado
        Reserva solapada = new Reserva(
                null, 13L, 3L, 2L, 3L,
                LocalDateTime.of(2026, 9, 10, 16, 30),
                60, null, new BigDecimal("55.00")
        );

        ReservaConflictException ex = assertThrows(ReservaConflictException.class, () ->
                reservaService.crearReserva(solapada)
        );
        assertTrue(ex.getMessage().contains("suite o sala de tratamiento seleccionada ya se encuentra ocupada"));
    }

    @Test
    @DisplayName("Debe permitir reservar en el mismo horario si la reserva previa fue CANCELADA")
    void debePermitirReservaEnMismoHorarioSiReservaPreviaFueCancelada() {
        // Reserva cancelada previa
        Reserva cancelada = new Reserva(
                null, 11L, 1L, 1L, 1L,
                LocalDateTime.of(2026, 9, 10, 18, 0),
                60, EstadoReserva.CANCELADA, new BigDecimal("65.00")
        );
        reservaRepository.save(cancelada);

        // Nueva reserva en el mismo horario y sala
        Reserva nueva = new Reserva(
                null, 14L, 1L, 1L, 1L,
                LocalDateTime.of(2026, 9, 10, 18, 0),
                60, null, new BigDecimal("65.00")
        );

        Reserva creada = reservaService.crearReserva(nueva);
        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
    }

    @Test
    @DisplayName("Debe transicionar exitosamente de PENDIENTE a CONFIRMADA")
    void debeConfirmarReservaExitosamente() {
        Reserva pendiente = new Reserva(
                null, 15L, 1L, 1L, 1L,
                LocalDateTime.of(2026, 9, 11, 10, 0),
                60, EstadoReserva.PENDIENTE, new BigDecimal("65.00")
        );
        Reserva guardada = reservaRepository.save(pendiente);

        Reserva confirmada = reservaService.confirmarReserva(guardada.getId());

        assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
    }

    @Test
    @DisplayName("Debe rechazar la confirmación si la reserva ya estaba CANCELADA")
    void debeRechazarConfirmacionSiReservaEstaCancelada() {
        Reserva cancelada = new Reserva(
                null, 15L, 1L, 1L, 1L,
                LocalDateTime.of(2026, 9, 11, 10, 0),
                60, EstadoReserva.CANCELADA, new BigDecimal("65.00")
        );
        Reserva guardada = reservaRepository.save(cancelada);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reservaService.confirmarReserva(guardada.getId())
        );
        assertTrue(ex.getMessage().contains("previamente cancelada"));
    }

    @Test
    @DisplayName("Debe permitir cancelar una reserva activa")
    void debeCancelarReservaExitosamente() {
        Reserva activa = new Reserva(
                null, 16L, 2L, 2L, 2L,
                LocalDateTime.of(2026, 9, 11, 12, 0),
                60, EstadoReserva.CONFIRMADA, new BigDecimal("55.00")
        );
        Reserva guardada = reservaRepository.save(activa);

        Reserva cancelada = reservaService.cancelarReserva(guardada.getId());

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
    }

    @Test
    @DisplayName("Debe rechazar la cancelación si la sesión ya fue COMPLETADA")
    void debeRechazarCancelacionSiReservaYaFueCompletada() {
        Reserva completada = new Reserva(
                null, 16L, 2L, 2L, 2L,
                LocalDateTime.of(2026, 9, 11, 12, 0),
                60, EstadoReserva.COMPLETADA, new BigDecimal("55.00")
        );
        Reserva guardada = reservaRepository.save(completada);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reservaService.cancelarReserva(guardada.getId())
        );
        assertTrue(ex.getMessage().contains("ya ha sido completada"));
    }

    @Test
    @DisplayName("Debe lanzar ReservaNotFoundException si se busca un ID inexistente")
    void debeLanzarExcepcionSiReservaNoExiste() {
        assertThrows(ReservaNotFoundException.class, () ->
                reservaService.buscarPorId(9999L)
        );
    }
}
