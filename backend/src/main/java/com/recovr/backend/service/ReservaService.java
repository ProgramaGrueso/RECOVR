package com.recovr.backend.service;

import com.recovr.backend.dto.PagoRequest;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.entity.Canje;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.CanjeRepository;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ciclo de vida de una reserva hecho por personas: crear, reprogramar, cancelar, cobrar, check-in, finalizar y
 * consultas. Las reglas de agenda están en {@link ValidadorAgenda}; los turnos libres, en {@link DisponibilidadService};
 * y los cambios automáticos (expiración, no-show), en {@link ReservaCicloVidaService}.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SalaRepository salaRepository;
    private final CanjeRepository canjeRepository;
    private final PuntosService puntosService;
    private final ValidadorAgenda validador;
    private final ReservaCicloVidaService cicloVida;
    private final UsuarioActualService usuarioActual;
    /** Reloj del negocio (hora de Lima): nunca la zona horaria del servidor. */
    private final Clock reloj;

    public ReservaService(ReservaRepository reservaRepository, PagoRepository pagoRepository,
                          ClienteRepository clienteRepository, EmpleadoRepository empleadoRepository,
                          SalaRepository salaRepository, CanjeRepository canjeRepository, PuntosService puntosService,
                          ValidadorAgenda validador, ReservaCicloVidaService cicloVida,
                          UsuarioActualService usuarioActual, Clock reloj) {
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.salaRepository = salaRepository;
        this.canjeRepository = canjeRepository;
        this.puntosService = puntosService;
        this.validador = validador;
        this.cicloVida = cicloVida;
        this.usuarioActual = usuarioActual;
        this.reloj = reloj;
    }

    /** Tamaño máximo de página del listado: el listado nunca devuelve la tabla completa. */
    public static final int TAMANO_MAX_LISTADO = 1000;

    /** Página del listado, de la más reciente a la más antigua. */
    public Page<Reserva> listarPagina(int pagina, int tamano) {
        int t = Math.max(1, Math.min(tamano, TAMANO_MAX_LISTADO));
        return reservaRepository.findAll(PageRequest.of(Math.max(0, pagina), t, Sort.by(Sort.Direction.DESC, "fechaHora")));
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }

    /**
     * Registra una reserva ya armada con sus entidades relacionadas.
     * Valida que el horario sea futuro y que no se solape con otra reserva activa
     * de la misma sala o del mismo especialista (duración + tiempo de limpieza), que caiga dentro del horario de
     * atención y que el cliente no tenga ya otra sesión cruzada ni acapare reservas sin pagar.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reserva crear(Reserva reserva) {
        if (reserva.getEstado() == null) {
            reserva.setEstado(EstadoReserva.PENDIENTE);
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (reserva.getFechaHora() == null || reserva.getFechaHora().isBefore(ahora)) {
            throw new IllegalArgumentException("La fecha y hora de la reserva debe ser futura");
        }
        validador.validarReglasDeAgenda(reserva);
        if (reserva.getCliente() != null) {
            validador.validarCliente(reserva, null, true, ahora);
        }
        validador.validarDisponibilidad(reserva, null);
        reserva.setCreadaEn(ahora);
        return reservaRepository.save(reserva);
    }

    /**
     * Crea una reserva a partir del DTO. Un CLIENTE siempre reserva a su nombre;
     * ADMIN y RECEPCIONISTA deben indicar el clienteId.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reserva crearDesdeRequest(ReservaRequest request, String correo, boolean puedeGestionar) {
        Reserva reserva = new Reserva();
        Long clienteId;
        if (puedeGestionar) {
            if (request.clienteId() == null) {
                throw new IllegalArgumentException("Debe indicar el clienteId de la reserva");
            }
            clienteId = request.clienteId();
        } else {
            clienteId = usuarioActual.clienteId(correo);
        }
        // Cola de atención: se bloquea en orden fijo cliente → especialista → sala. Dos reservas simultáneas que
        // compiten por el mismo recurso se procesan una tras otra; la segunda ve a la primera y recibe 409.
        reserva.setCliente(clienteRepository.buscarBloqueado(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId)));
        cicloVida.expirarPlazosVencidos(LocalDateTime.now(reloj));
        validador.asignarRelaciones(reserva, request);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        return crear(reserva);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reserva actualizar(Long id, ReservaRequest request) {
        Reserva reserva = reservaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
        if (!ReglasReserva.activa(reserva)) {
            throw new IllegalStateException("Una reserva " + reserva.getEstado()
                    + " ya no se puede editar; cree una reserva nueva.");
        }
        if (reserva.getCheckIn() != null) {
            throw new IllegalStateException("La sesión ya tiene check-in: no se puede modificar.");
        }
        // El estado no se cambia a mano: confirmar exige el pago completo y completar exige el check-in.
        // Lo único que se admite por aquí es cancelar, y eso pasa por la misma regla (con crédito si había pago).
        EstadoReserva nuevo = request.estado();
        if (nuevo != null && nuevo != reserva.getEstado()) {
            if (nuevo != EstadoReserva.CANCELADA) {
                throw new IllegalStateException("El estado no se cambia a mano (" + reserva.getEstado() + " → " + nuevo
                        + "): use confirmar-y-pagar, check-in o finalizar. Por aquí solo se puede cancelar.");
            }
            aplicarCancelacion(reserva, true, LocalDateTime.now(reloj));
            return reservaRepository.save(reserva);
        }
        LocalDateTime fechaAnterior = reserva.getFechaHora();
        Long servicioAnterior = reserva.getServicio().getId();
        Long clienteId = request.clienteId() != null ? request.clienteId() : reserva.getCliente().getId();
        reserva.setCliente(clienteRepository.buscarBloqueado(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId)));
        cicloVida.expirarPlazosVencidos(LocalDateTime.now(reloj));
        validador.asignarRelaciones(reserva, request);
        if (reserva.getPago() != null && !reserva.getServicio().getId().equals(servicioAnterior)) {
            throw new IllegalStateException("La sesión ya está pagada: no se puede cambiar de servicio. "
                    + "Cancélela (el pago queda como crédito) y cree una nueva.");
        }
        if (!reserva.getFechaHora().equals(fechaAnterior) && reserva.getFechaHora().isBefore(LocalDateTime.now(reloj))) {
            throw new IllegalArgumentException("La nueva fecha y hora de la reserva debe ser futura");
        }
        validador.validarReglasDeAgenda(reserva);
        validador.validarCliente(reserva, reserva.getId(), false, LocalDateTime.now(reloj));
        validador.validarDisponibilidad(reserva, reserva.getId());
        return reservaRepository.save(reserva);
    }

    /**
     * Cancela una reserva cambiando su estado (no borra la fila: se conserva el historial).
     * Un CLIENTE solo cancela las suyas; ADMIN y RECEPCIONISTA cancelan cualquiera.
     * Solo se pueden cancelar reservas PENDIENTE o CONFIRMADA.
     */
    @Transactional
    public Reserva cancelar(Long id, String correo, boolean puedeGestionar) {
        Reserva reserva = reservaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
        if (!puedeGestionar && !reserva.getCliente().getId().equals(usuarioActual.clienteId(correo))) {
            throw new AccessDeniedException("Un cliente solo puede cancelar sus propias reservas");
        }
        if (!ReglasReserva.activa(reserva)) {
            throw new IllegalStateException("Solo se pueden cancelar reservas PENDIENTE o CONFIRMADA (estado actual: "
                    + reserva.getEstado() + ")");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!puedeGestionar) {
            // Política para el cliente: anticipación mínima y tope mensual de cancelaciones propias
            if (ahora.isAfter(reserva.getFechaHora().minusHours(ReglasReserva.HORAS_MIN_CANCELACION_CLIENTE))) {
                throw new IllegalStateException("Faltan menos de " + ReglasReserva.HORAS_MIN_CANCELACION_CLIENTE
                        + " horas para tu sesión: ya no puedes cancelarla por tu cuenta. Comunícate con recepción.");
            }
            long delMes = reservaRepository.countByClienteIdAndObservacionAndCanceladaEnGreaterThanEqual(
                    reserva.getCliente().getId(), ReglasReserva.CANCELADA_POR_CLIENTE,
                    ahora.toLocalDate().withDayOfMonth(1).atStartOfDay());
            if (delMes >= ReglasReserva.MAX_CANCELACIONES_CLIENTE_MES) {
                throw new IllegalStateException("Alcanzaste el límite de " + ReglasReserva.MAX_CANCELACIONES_CLIENTE_MES
                        + " cancelaciones por mes. Comunícate con recepción.");
            }
        }
        aplicarCancelacion(reserva, puedeGestionar, ahora);
        return reservaRepository.save(reserva);
    }

    /**
     * Cancela y deja al cliente sin pérdida: un código de canje que la reserva había consumido vuelve a quedar
     * disponible y el dinero ya pagado se convierte en un crédito (código de un solo uso) por el mismo importe.
     * No aplica al no-show ni a la expiración automática: esas las decide el planificador, no una cancelación.
     */
    private void aplicarCancelacion(Reserva reserva, boolean porPersonal, LocalDateTime ahora) {
        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setCanceladaEn(ahora);
        reserva.setObservacion(porPersonal ? ReglasReserva.CANCELADA_POR_PERSONAL : ReglasReserva.CANCELADA_POR_CLIENTE);
        puntosService.liberarCanjesDe(reserva);
        if (reserva.getPago() != null && reserva.getPago().getMonto() != null && reserva.getPago().getMonto().signum() > 0) {
            puntosService.crearCredito(reserva.getCliente(), reserva.getPago().getMonto(),
                    "Crédito por la cancelación de la reserva #" + reserva.getId());
        }
    }

    /** Solo se borran reservas CANCELADAS; las COMPLETADAS son historial contable y las activas se cancelan primero. */
    @Transactional
    public void eliminar(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva.getEstado() != EstadoReserva.CANCELADA) {
            throw new IllegalStateException("Solo se pueden eliminar reservas CANCELADAS (estado actual: "
                    + reserva.getEstado() + "). Cancele la reserva primero; las completadas se conservan como historial.");
        }
        reservaRepository.delete(reserva);
    }

    /**
     * El cliente llegó tarde y el planificador ya lo había dado por no asistido: recepción puede reabrir la reserva
     * (CONFIRMADA otra vez) mientras la sesión no haya terminado y el cupo siga libre. Solo aplica a no-shows de una
     * sesión pagada; una cancelación decidida por una persona no se reabre.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reserva reabrirNoAsistio(Long id) {
        Reserva reserva = reservaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
        if (reserva.getEstado() != EstadoReserva.CANCELADA || !ReglasReserva.NO_ASISTIO.equals(reserva.getObservacion())
                || reserva.getPago() == null) {
            throw new IllegalStateException("Solo se puede reabrir una sesión pagada que se canceló automáticamente por no asistencia.");
        }
        if (!LocalDateTime.now(reloj).isBefore(ReglasReserva.finSesion(reserva))) {
            throw new IllegalStateException("La sesión ya terminó; no se puede reabrir.");
        }
        clienteRepository.buscarBloqueado(reserva.getCliente().getId());
        empleadoRepository.buscarBloqueado(reserva.getEmpleado().getId());
        salaRepository.buscarBloqueada(reserva.getSala().getId());
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        validador.validarDisponibilidad(reserva, reserva.getId());
        validador.validarCliente(reserva, reserva.getId(), false, LocalDateTime.now(reloj));
        reserva.setCanceladaEn(null);
        reserva.setObservacion("Reabierta por el personal: el cliente llegó tarde.");
        return reservaRepository.save(reserva);
    }

    public Reserva checkIn(Long id) {
        return checkIn(id, LocalDateTime.now(reloj));
    }

    /**
     * Registra la llegada del cliente. Requisitos: reserva CONFIRMADA (pagada), sin check-in previo y dentro de la
     * ventana [inicio − {@value ReglasReserva#CHECKIN_ANTICIPADO_MIN} min, fin de la sesión).
     */
    @Transactional
    public Reserva checkIn(Long id, LocalDateTime ahora) {
        Reserva reserva = buscarPorId(id);
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException("Solo una reserva CONFIRMADA (pagada) puede hacer check-in (estado actual: "
                    + reserva.getEstado() + ")");
        }
        if (reserva.getCheckIn() != null) {
            throw new IllegalStateException("La reserva ya tiene check-in registrado");
        }
        LocalDateTime desde = reserva.getFechaHora().minusMinutes(ReglasReserva.CHECKIN_ANTICIPADO_MIN);
        if (ahora.isBefore(desde)) {
            throw new IllegalStateException("Todavía es pronto: el check-in se habilita " + ReglasReserva.CHECKIN_ANTICIPADO_MIN
                    + " minutos antes (desde las " + desde.toLocalTime().withSecond(0).withNano(0) + ")");
        }
        if (!ahora.isBefore(ReglasReserva.finSesion(reserva))) {
            throw new IllegalStateException("La sesión ya terminó; no se puede registrar el check-in");
        }
        reserva.setCheckIn(ahora);
        return reservaRepository.save(reserva);
    }

    /** Cierra la sesión manualmente (antes del cierre automático): exige check-in previo. */
    @Transactional
    public Reserva finalizar(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA || reserva.getCheckIn() == null) {
            throw new IllegalStateException("Solo se puede finalizar una sesión CONFIRMADA con check-in (estado actual: "
                    + ReglasReserva.estadoOperativo(reserva) + ")");
        }
        reserva.setEstado(EstadoReserva.COMPLETADA);
        reserva.setObservacion("Finalizada manualmente por el personal.");
        return reservaRepository.save(reserva);
    }

    public List<Reserva> buscarMisReservas(String correo) {
        return reservaRepository.buscarPorCliente(usuarioActual.clienteId(correo));
    }

    public List<Reserva> buscarPorClienteParaUsuario(Long clienteId, String correo, boolean puedeVerTodas) {
        if (!puedeVerTodas && !clienteId.equals(usuarioActual.clienteId(correo))) {
            throw new AccessDeniedException("Un cliente solo puede consultar sus propias reservas");
        }
        return reservaRepository.buscarPorCliente(clienteId);
    }


    public List<Reserva> buscarPorRangoYEstado(LocalDateTime inicio, LocalDateTime fin, EstadoReserva estado) {
        validarRango(inicio, fin);
        return reservaRepository.buscarPorRangoYEstado(inicio, fin, estado);
    }

    /** Agenda futura del especialista vinculado al usuario autenticado. */
    public List<Reserva> buscarAgendaEspecialista(String correo) {
        Empleado empleado = usuarioActual.empleado(correo, "El usuario no está vinculado a ningún especialista");
        return reservaRepository.buscarAgendaEmpleado(empleado.getId(), LocalDateTime.now(reloj));
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
        if (ReglasReserva.holdVencido(reserva, LocalDateTime.now(reloj))) {
            throw new IllegalStateException("El plazo para pagar (" + ReglasReserva.HOLD_PAGO_MIN
                    + " min) venció y el cupo se liberó. Crea la reserva de nuevo.");
        }
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        pago.setReserva(reserva);
        reserva.setPago(pago);
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(LocalDateTime.now(reloj));
        }
        pagoRepository.save(pago);

        return reserva;
    }

    /**
     * Cobra y confirma. Solo el pago total asegura el turno: el monto debe ser el precio del servicio (menos el
     * código de canje, que decide el servidor). La fila de la reserva se bloquea, así dos cobros simultáneos de la
     * misma reserva se hacen en fila y el segundo falla por no estar ya PENDIENTE.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reserva confirmarYPagarParaUsuario(Long reservaId, PagoRequest request, String correo, boolean puedeGestionar) {
        Reserva reserva = reservaRepository.buscarParaActualizar(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", reservaId));
        if (!puedeGestionar && !reserva.getCliente().getId().equals(usuarioActual.clienteId(correo))) {
            throw new AccessDeniedException("Un cliente solo puede pagar sus propias reservas");
        }
        BigDecimal precio = reserva.getServicio().getPrecio();
        BigDecimal monto = request.monto();
        Canje canje = null;
        if (request.codigoCanje() != null && !request.codigoCanje().isBlank()) {
            // Con código de canje el servidor decide el monto: precio − descuento (nunca menos de 0)
            canje = puntosService.canjeParaCobro(request.codigoCanje(), reserva.getCliente());
            monto = precio.subtract(canje.getDescuento()).max(BigDecimal.ZERO);
        } else if (monto == null || monto.compareTo(precio) != 0) {
            throw new IllegalArgumentException("El pago debe ser el total de la sesión (S/ " + precio.toPlainString()
                    + "): solo el pago completo asegura el turno.");
        }
        Pago pago = new Pago();
        pago.setMonto(monto);
        pago.setMetodoPago(canje != null && monto.signum() == 0 ? "Canje de puntos" : request.metodoPago());
        Reserva confirmada = confirmarYPagar(reservaId, pago);
        if (canje != null) {
            canje.setEstado(Canje.Estado.USADO);
            canje.setUsadoEn(LocalDateTime.now(reloj));
            canje.setReserva(confirmada);
            canjeRepository.save(canje);
            // Un crédito mayor que la sesión no se pierde: lo que sobra queda como un crédito nuevo
            if (ReglasReserva.CREDITO_CANCELACION.equals(canje.getBeneficioId()) && canje.getDescuento().compareTo(precio) > 0) {
                puntosService.crearCredito(confirmada.getCliente(), canje.getDescuento().subtract(precio),
                        "Saldo de crédito sin usar (" + canje.getCodigo() + ")");
            }
        }
        return confirmada;
    }










    static void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }


}
