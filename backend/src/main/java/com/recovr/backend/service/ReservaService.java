package com.recovr.backend.service;

import com.recovr.backend.dto.PagoRequest;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.dto.ReservaResponse;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.exception.ReservaConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service("jpaReservaService")
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private SalaRepository salaRepository;

    public List<Reserva> listarTodos() {
        return reservaRepository.findAll();
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }

    /**
     * Registra una reserva ya armada con sus entidades relacionadas.
     * Valida que el horario sea futuro y que no se solape con otra reserva activa
     * de la misma sala o del mismo especialista (duración + tiempo de limpieza).
     */
    @Transactional
    public Reserva crear(Reserva reserva) {
        if (reserva.getEstado() == null) {
            reserva.setEstado(EstadoReserva.PENDIENTE);
        }
        if (reserva.getFechaHora() == null || reserva.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora de la reserva debe ser futura");
        }
        validarDisponibilidad(reserva, null);
        return reservaRepository.save(reserva);
    }

    /**
     * Crea una reserva a partir del DTO. Un CLIENTE siempre reserva a su nombre;
     * ADMIN y RECEPCIONISTA deben indicar el clienteId.
     */
    @Transactional
    public Reserva crearDesdeRequest(ReservaRequest request, String correo, boolean puedeGestionar) {
        Reserva reserva = new Reserva();
        if (puedeGestionar) {
            if (request.clienteId() == null) {
                throw new IllegalArgumentException("Debe indicar el clienteId de la reserva");
            }
            reserva.setCliente(clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId())));
        } else {
            reserva.setCliente(clienteRepository.findById(clienteIdDelUsuario(correo)).orElseThrow());
        }
        asignarRelaciones(reserva, request);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        return crear(reserva);
    }

    @Transactional
    public Reserva actualizar(Long id, ReservaRequest request) {
        Reserva reserva = buscarPorId(id);
        if (request.clienteId() != null) {
            reserva.setCliente(clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId())));
        }
        asignarRelaciones(reserva, request);
        if (request.estado() != null) {
            reserva.setEstado(request.estado());
        }
        if (estaActiva(reserva)) {
            validarDisponibilidad(reserva, reserva.getId());
        }
        return reservaRepository.save(reserva);
    }

    public void eliminar(Long id) {
        if (!reservaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Reserva", id);
        }
        reservaRepository.deleteById(id);
    }

    public List<Reserva> buscarMisReservas(String correo) {
        return reservaRepository.buscarPorCliente(clienteIdDelUsuario(correo));
    }

    public List<Reserva> buscarPorClienteParaUsuario(Long clienteId, String correo, boolean puedeVerTodas) {
        if (!puedeVerTodas && !clienteId.equals(clienteIdDelUsuario(correo))) {
            throw new AccessDeniedException("Un cliente solo puede consultar sus propias reservas");
        }
        return reservaRepository.buscarPorCliente(clienteId);
    }

    public List<Reserva> buscarPorCliente(Long clienteId) {
        return reservaRepository.buscarPorCliente(clienteId);
    }

    public List<Reserva> buscarPorRangoYEstado(LocalDateTime inicio, LocalDateTime fin, EstadoReserva estado) {
        validarRango(inicio, fin);
        return reservaRepository.buscarPorRangoYEstado(inicio, fin, estado);
    }

    /** Agenda futura del especialista vinculado al usuario autenticado. */
    public List<Reserva> buscarAgendaEspecialista(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Empleado empleado = empleadoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("El usuario no está vinculado a ningún especialista"));
        return reservaRepository.buscarAgendaEmpleado(empleado.getId(), LocalDateTime.now());
    }

    // Ejemplo de transaccion: confirma la reserva y registra su pago en una sola operacion.
    // Si algo falla a mitad de camino, se revierte todo (ni la reserva queda confirmada
    // ni el pago se guarda a medias).
    @Transactional
    public Reserva confirmarYPagar(Long reservaId, Pago pago) {
        Reserva reserva = buscarPorId(reservaId);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden confirmar reservas en estado PENDIENTE (estado actual: "
                    + reserva.getEstado() + ")");
        }
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        pago.setReserva(reserva);
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(LocalDateTime.now());
        }
        pagoRepository.save(pago);

        return reserva;
    }

    @Transactional
    public Reserva confirmarYPagarParaUsuario(Long reservaId, PagoRequest request, String correo, boolean puedeGestionar) {
        Reserva reserva = buscarPorId(reservaId);
        if (!puedeGestionar && !reserva.getCliente().getId().equals(clienteIdDelUsuario(correo))) {
            throw new AccessDeniedException("Un cliente solo puede pagar sus propias reservas");
        }
        Pago pago = new Pago();
        pago.setMonto(request.monto());
        pago.setMetodoPago(request.metodoPago());
        return confirmarYPagar(reservaId, pago);
    }

    /**
     * Regla de negocio central (misma que el Core Engine del APF1): ningún especialista ni sala
     * puede tener dos reservas activas cuyos bloques [inicio, inicio + duración + limpieza) se crucen.
     */
    private void validarDisponibilidad(Reserva reserva, Long excluirId) {
        if (reserva.getSala() == null || reserva.getEmpleado() == null || reserva.getServicio() == null) {
            throw new IllegalArgumentException("La reserva debe indicar sala, especialista y servicio");
        }
        LocalDateTime inicio = reserva.getFechaHora();
        LocalDateTime fin = ReservaResponse.finBloque(inicio, reserva.getServicio());
        List<Reserva> conflictos = reservaRepository.buscarConflictos(
                reserva.getSala().getId(), reserva.getEmpleado().getId(), inicio, fin, excluirId);

        boolean especialistaOcupado = conflictos.stream()
                .anyMatch(r -> r.getEmpleado().getId().equals(reserva.getEmpleado().getId()));
        if (especialistaOcupado) {
            throw new ReservaConflictException("El especialista seleccionado no se encuentra disponible en el turno solicitado.");
        }
        if (!conflictos.isEmpty()) {
            throw new ReservaConflictException("La suite o sala de tratamiento seleccionada ya se encuentra ocupada.");
        }
    }

    private void asignarRelaciones(Reserva reserva, ReservaRequest request) {
        reserva.setEmpleado(empleadoRepository.findById(request.empleadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", request.empleadoId())));
        reserva.setServicio(servicioRepository.findById(request.servicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio", request.servicioId())));
        reserva.setSala(salaRepository.findById(request.salaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sala", request.salaId())));
        reserva.setFechaHora(request.fechaHora());
    }

    private boolean estaActiva(Reserva r) {
        return r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.CONFIRMADA;
    }

    static void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }

    private Long clienteIdDelUsuario(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("El usuario no tiene un cliente asociado"))
                .getId();
    }
}
