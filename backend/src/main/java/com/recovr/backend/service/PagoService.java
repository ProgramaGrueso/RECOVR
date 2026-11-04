package com.recovr.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.recovr.backend.dto.PagoRequest;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ReservaService reservaService;

    public PagoService(PagoRepository pagoRepository, ReservaService reservaService) {
        this.pagoRepository = pagoRepository;
        this.reservaService = reservaService;
    }

    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    /** Página de pagos, del más reciente al más antiguo. */
    public Page<Pago> listarPagina(int pagina, int tamano) {
        int t = Math.max(1, Math.min(tamano, 1000));
        return pagoRepository.findAll(PageRequest.of(Math.max(0, pagina), t, Sort.by(Sort.Direction.DESC, "fechaPago", "id")));
    }

    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
    }

    /**
     * Registra el pago de una reserva PENDIENTE (ADMIN). Sigue exactamente las mismas reglas que el cobro normal:
     * debe ser el total de la sesión y deja la reserva CONFIRMADA en la misma transacción; un pago suelto sobre una
     * reserva que no se confirma no tiene sentido y dejaba la sesión "pagada" pero sin asegurar.
     */
    public Pago crear(PagoRequest request) {
        if (request.reservaId() == null) {
            throw new IllegalArgumentException("Debe indicar el reservaId del pago");
        }
        if (pagoRepository.existsByReservaId(request.reservaId())) {
            throw new IllegalStateException("La reserva " + request.reservaId() + " ya tiene un pago registrado");
        }
        reservaService.confirmarYPagarParaUsuario(request.reservaId(), request, null, true);
        return pagoRepository.findByReservaId(request.reservaId()).orElseThrow();
    }

    /** El pago de una sesión confirmada o completada es historial contable: no se borra (cancelar la reserva genera el crédito). */
    public void eliminar(Long id) {
        Pago pago = pagoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
        EstadoReserva estado = pago.getReserva().getEstado();
        if (estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.COMPLETADA) {
            throw new IllegalStateException("No se puede eliminar el pago de una reserva " + estado
                    + ": es historial contable. Para devolver el dinero cancele la reserva y el cliente recibe un crédito.");
        }
        pagoRepository.deleteById(id);
    }
}
