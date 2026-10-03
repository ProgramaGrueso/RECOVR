package com.recovr.backend.service;

import com.recovr.backend.dto.PagoRequest;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoService {

    @Autowired
    private PagoRepository pagoRepository;

    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
    }

    @Autowired
    private ReservaRepository reservaRepository;

    /** Registra un pago directo (ADMIN). Cada reserva admite un único pago. */
    public Pago crear(PagoRequest request) {
        if (request.reservaId() == null) {
            throw new IllegalArgumentException("Debe indicar el reservaId del pago");
        }
        Reserva reserva = reservaRepository.findById(request.reservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", request.reservaId()));
        if (pagoRepository.existsByReservaId(reserva.getId())) {
            throw new IllegalStateException("La reserva " + reserva.getId() + " ya tiene un pago registrado");
        }
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMonto(request.monto());
        pago.setMetodoPago(request.metodoPago());
        pago.setFechaPago(LocalDateTime.now());
        return pagoRepository.save(pago);
    }

    public void eliminar(Long id) {
        if (!pagoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Pago", id);
        }
        pagoRepository.deleteById(id);
    }
}
