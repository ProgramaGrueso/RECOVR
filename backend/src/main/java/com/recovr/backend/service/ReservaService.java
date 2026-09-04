package com.recovr.backend.service;

import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    public List<Reserva> listarTodos() {
        return reservaRepository.findAll();
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id " + id));
    }

    public Reserva crear(Reserva reserva) {
        if (reserva.getEstado() == null) {
            reserva.setEstado(EstadoReserva.PENDIENTE);
        }
        return reservaRepository.save(reserva);
    }

    public Reserva actualizar(Long id, Reserva datos) {
        Reserva reserva = buscarPorId(id);
        reserva.setCliente(datos.getCliente());
        reserva.setEmpleado(datos.getEmpleado());
        reserva.setServicio(datos.getServicio());
        reserva.setSala(datos.getSala());
        reserva.setFechaHora(datos.getFechaHora());
        reserva.setEstado(datos.getEstado());
        return reservaRepository.save(reserva);
    }

    public void eliminar(Long id) {
        reservaRepository.deleteById(id);
    }

    public List<Reserva> buscarPorCliente(Long clienteId) {
        return reservaRepository.buscarPorCliente(clienteId);
    }

    public List<Reserva> buscarPorRangoYEstado(LocalDateTime inicio, LocalDateTime fin, EstadoReserva estado) {
        return reservaRepository.buscarPorRangoYEstado(inicio, fin, estado);
    }

    // Ejemplo de transaccion: confirma la reserva y registra su pago en una sola operacion.
    // Si algo falla a mitad de camino, se revierte todo (ni la reserva queda confirmada
    // ni el pago se guarda a medias).
    @Transactional
    public Reserva confirmarYPagar(Long reservaId, Pago pago) {
        Reserva reserva = buscarPorId(reservaId);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        pago.setReserva(reserva);
        pagoRepository.save(pago);

        return reserva;
    }
}
