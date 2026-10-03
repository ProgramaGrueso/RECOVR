package com.recovr.backend.service;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.exception.ReservaConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReservaBackendServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Reserva reservaPrueba;
    private Sala sala;
    private Empleado empleado;
    private Servicio servicio;
    private LocalDateTime manana10;

    @BeforeEach
    void setUp() {
        manana10 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        sala = new Sala();
        sala.setId(1L);
        empleado = new Empleado();
        empleado.setId(1L);
        servicio = new Servicio();
        servicio.setId(1L);
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);

        reservaPrueba = new Reserva();
        reservaPrueba.setId(10L);
        reservaPrueba.setFechaHora(manana10);
        reservaPrueba.setEstado(EstadoReserva.PENDIENTE);
    }

    private Reserva nuevaReserva(LocalDateTime inicio) {
        Reserva r = new Reserva();
        r.setSala(sala);
        r.setEmpleado(empleado);
        r.setServicio(servicio);
        r.setFechaHora(inicio);
        return r;
    }

    @Test
    @DisplayName("Debe asignar estado PENDIENTE por defecto al crear una reserva sin estado")
    void debeAsignarEstadoPendienteAlCrear() {
        Reserva reservaSinEstado = nuevaReserva(manana10);
        given(reservaRepository.buscarConflictos(any(), any(), any(), any(), any())).willReturn(List.of());
        given(reservaRepository.save(any(Reserva.class))).willAnswer(invocation -> invocation.getArgument(0));

        Reserva creada = reservaService.crear(reservaSinEstado);

        assertThat(creada.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        verify(reservaRepository).save(reservaSinEstado);
    }

    @Test
    @DisplayName("Debe consultar conflictos con el bloque completo: duración + limpieza")
    void debeConsultarConflictosConBloqueCompleto() {
        given(reservaRepository.buscarConflictos(any(), any(), any(), any(), any())).willReturn(List.of());
        given(reservaRepository.save(any(Reserva.class))).willAnswer(invocation -> invocation.getArgument(0));

        reservaService.crear(nuevaReserva(manana10));

        verify(reservaRepository).buscarConflictos(eq(1L), eq(1L), eq(manana10), eq(manana10.plusMinutes(75)), isNull());
    }

    @Test
    @DisplayName("Debe rechazar con conflicto si el especialista ya está ocupado")
    void debeRechazarEspecialistaOcupado() {
        Reserva existente = nuevaReserva(manana10);
        existente.setId(5L);
        given(reservaRepository.buscarConflictos(any(), any(), any(), any(), any())).willReturn(List.of(existente));

        assertThatThrownBy(() -> reservaService.crear(nuevaReserva(manana10.plusMinutes(30))))
                .isInstanceOf(ReservaConflictException.class)
                .hasMessageContaining("especialista");
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar con conflicto si solo la sala está ocupada (otro especialista)")
    void debeRechazarSalaOcupada() {
        Empleado otro = new Empleado();
        otro.setId(2L);
        Reserva existente = nuevaReserva(manana10);
        existente.setEmpleado(otro);
        given(reservaRepository.buscarConflictos(any(), any(), any(), any(), any())).willReturn(List.of(existente));

        assertThatThrownBy(() -> reservaService.crear(nuevaReserva(manana10)))
                .isInstanceOf(ReservaConflictException.class)
                .hasMessageContaining("sala");
    }

    @Test
    @DisplayName("Debe rechazar reservas con fecha en el pasado")
    void debeRechazarFechaPasada() {
        assertThatThrownBy(() -> reservaService.crear(nuevaReserva(LocalDateTime.now().minusHours(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Debe confirmar reserva y registrar su pago de forma transaccional")
    void debeConfirmarYPagar() {
        given(reservaRepository.findById(10L)).willReturn(Optional.of(reservaPrueba));
        given(reservaRepository.save(any(Reserva.class))).willReturn(reservaPrueba);

        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("150.00"));
        pago.setMetodoPago("TARJETA");
        given(pagoRepository.save(any(Pago.class))).willReturn(pago);

        Reserva resultado = reservaService.confirmarYPagar(10L, pago);

        assertThat(resultado.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(pago.getFechaPago()).isNotNull();
        verify(reservaRepository).save(reservaPrueba);
        verify(pagoRepository).save(pago);
    }

    @Test
    @DisplayName("No debe confirmar una reserva que no está PENDIENTE")
    void noDebeConfirmarReservaCancelada() {
        reservaPrueba.setEstado(EstadoReserva.CANCELADA);
        given(reservaRepository.findById(10L)).willReturn(Optional.of(reservaPrueba));

        assertThatThrownBy(() -> reservaService.confirmarYPagar(10L, new Pago()))
                .isInstanceOf(IllegalStateException.class);
        verify(pagoRepository, never()).save(any());
    }
}
