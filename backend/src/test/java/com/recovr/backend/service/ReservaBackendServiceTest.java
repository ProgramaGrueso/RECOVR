package com.recovr.backend.service;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

    @BeforeEach
    void setUp() {
        reservaPrueba = new Reserva();
        reservaPrueba.setId(10L);
        reservaPrueba.setFechaHora(LocalDateTime.now());
        reservaPrueba.setEstado(EstadoReserva.PENDIENTE);
    }

    @Test
    @DisplayName("Debe asignar estado PENDIENTE por defecto al crear una reserva sin estado")
    void debeAsignarEstadoPendienteAlCrear() {
        Reserva reservaSinEstado = new Reserva();
        reservaSinEstado.setId(11L);
        reservaSinEstado.setEstado(null);

        given(reservaRepository.save(any(Reserva.class))).willAnswer(invocation -> invocation.getArgument(0));

        Reserva creada = reservaService.crear(reservaSinEstado);

        assertThat(creada.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        verify(reservaRepository).save(reservaSinEstado);
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
        verify(reservaRepository).save(reservaPrueba);
        verify(pagoRepository).save(pago);
    }
}
