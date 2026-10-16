package com.recovr.backend.service;

import com.recovr.backend.dto.PagoRequest;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.dto.ReservaResponse;
import com.recovr.backend.entity.Canje;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.dto.TurnoDisponibleResponse;
import com.recovr.backend.repository.AusenciaEmpleadoRepository;
import com.recovr.backend.repository.CanjeRepository;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.exception.ReservaBloqueadaException;
import com.recovr.backend.exception.ReservaConflictException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServicioRepository servicioRepository;
    private final SalaRepository salaRepository;
    private final PuntosService puntosService;
    private final CanjeRepository canjeRepository;
    private final AusenciaEmpleadoRepository ausenciaRepository;

    public ReservaService(ReservaRepository reservaRepository, PagoRepository pagoRepository, UsuarioRepository usuarioRepository, ClienteRepository clienteRepository, EmpleadoRepository empleadoRepository, ServicioRepository servicioRepository, SalaRepository salaRepository, PuntosService puntosService, CanjeRepository canjeRepository, AusenciaEmpleadoRepository ausenciaRepository) {
        this.ausenciaRepository = ausenciaRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.servicioRepository = servicioRepository;
        this.salaRepository = salaRepository;
        this.puntosService = puntosService;
        this.canjeRepository = canjeRepository;
    }

    public List<Reserva> listarTodos() {
        return reservaRepository.findAll();
    }

    /** Tamaño máximo de página del listado: el listado nunca devuelve la tabla completa. */
    public static final int TAMANO_MAX_LISTADO = 1000;

    /** Página del listado, de la más reciente a la más antigua. */
    public org.springframework.data.domain.Page<Reserva> listarPagina(int pagina, int tamano) {
        int t = Math.max(1, Math.min(tamano, TAMANO_MAX_LISTADO));
        return reservaRepository.findAll(org.springframework.data.domain.PageRequest.of(Math.max(0, pagina), t,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaHora")));
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
        LocalDateTime ahora = LocalDateTime.now();
        if (reserva.getFechaHora() == null || reserva.getFechaHora().isBefore(ahora)) {
            throw new IllegalArgumentException("La fecha y hora de la reserva debe ser futura");
        }
        validarReglasDeAgenda(reserva, null);
        if (reserva.getCliente() != null) {
            validarCliente(reserva, null, true, ahora);
        }
        validarDisponibilidad(reserva, null);
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
            clienteId = clienteIdDelUsuario(correo);
        }
        // Cola de atención: se bloquea en orden fijo cliente → especialista → sala. Dos reservas simultáneas que
        // compiten por el mismo recurso se procesan una tras otra; la segunda ve a la primera y recibe 409.
        reserva.setCliente(clienteRepository.buscarBloqueado(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId)));
        liberarPlazosVencidos(LocalDateTime.now());
        asignarRelaciones(reserva, request);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        return crear(reserva);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reserva actualizar(Long id, ReservaRequest request) {
        Reserva reserva = reservaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
        if (!estaActiva(reserva)) {
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
            aplicarCancelacion(reserva, true, LocalDateTime.now());
            return reservaRepository.save(reserva);
        }
        LocalDateTime fechaAnterior = reserva.getFechaHora();
        Long servicioAnterior = reserva.getServicio().getId();
        Long clienteId = request.clienteId() != null ? request.clienteId() : reserva.getCliente().getId();
        reserva.setCliente(clienteRepository.buscarBloqueado(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId)));
        liberarPlazosVencidos(LocalDateTime.now());
        asignarRelaciones(reserva, request);
        if (reserva.getPago() != null && !reserva.getServicio().getId().equals(servicioAnterior)) {
            throw new IllegalStateException("La sesión ya está pagada: no se puede cambiar de servicio. "
                    + "Cancélela (el pago queda como crédito) y cree una nueva.");
        }
        if (!reserva.getFechaHora().equals(fechaAnterior) && reserva.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha y hora de la reserva debe ser futura");
        }
        validarReglasDeAgenda(reserva, reserva.getId());
        validarCliente(reserva, reserva.getId(), false, LocalDateTime.now());
        validarDisponibilidad(reserva, reserva.getId());
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
        if (!puedeGestionar && !reserva.getCliente().getId().equals(clienteIdDelUsuario(correo))) {
            throw new AccessDeniedException("Un cliente solo puede cancelar sus propias reservas");
        }
        if (!estaActiva(reserva)) {
            throw new IllegalStateException("Solo se pueden cancelar reservas PENDIENTE o CONFIRMADA (estado actual: "
                    + reserva.getEstado() + ")");
        }
        LocalDateTime ahora = LocalDateTime.now();
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
        if (!LocalDateTime.now().isBefore(ReglasReserva.finSesion(reserva))) {
            throw new IllegalStateException("La sesión ya terminó; no se puede reabrir.");
        }
        clienteRepository.buscarBloqueado(reserva.getCliente().getId());
        empleadoRepository.buscarBloqueado(reserva.getEmpleado().getId());
        salaRepository.buscarBloqueada(reserva.getSala().getId());
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        validarDisponibilidad(reserva, reserva.getId());
        validarCliente(reserva, reserva.getId(), false, LocalDateTime.now());
        reserva.setCanceladaEn(null);
        reserva.setObservacion("Reabierta por el personal: el cliente llegó tarde.");
        return reservaRepository.save(reserva);
    }

    public Reserva checkIn(Long id) {
        return checkIn(id, LocalDateTime.now());
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
        if (ReglasReserva.holdVencido(reserva, LocalDateTime.now())) {
            throw new IllegalStateException("El plazo para pagar (" + ReglasReserva.HOLD_PAGO_MIN
                    + " min) venció y el cupo se liberó. Crea la reserva de nuevo.");
        }
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        pago.setReserva(reserva);
        reserva.setPago(pago);
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(LocalDateTime.now());
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
        if (!puedeGestionar && !reserva.getCliente().getId().equals(clienteIdDelUsuario(correo))) {
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
            canje.setUsadoEn(LocalDateTime.now());
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

    /** Datos mínimos para poder evaluar cualquier regla de agenda. */
    private void exigirDatosCompletos(Reserva reserva) {
        if (reserva.getSala() == null || reserva.getEmpleado() == null || reserva.getServicio() == null) {
            throw new IllegalArgumentException("La reserva debe indicar sala, especialista y servicio");
        }
    }

    /** La sesión completa debe caber en el horario de atención (el mismo que usa la ocupación diaria). */
    private void validarReglasDeAgenda(Reserva reserva, Long excluirId) {
        exigirDatosCompletos(reserva);
        LocalDateTime inicio = reserva.getFechaHora();
        LocalDateTime finSesion = ReglasReserva.finSesion(reserva);
        if (!ReglasReserva.dentroDeHorario(inicio, finSesion)) {
            throw new IllegalArgumentException("La sesión (" + inicio.toLocalTime().withSecond(0).withNano(0) + " a "
                    + finSesion.toLocalTime().withSecond(0).withNano(0) + ") debe estar dentro del horario de atención: "
                    + ReglasReserva.APERTURA + " a " + ReglasReserva.CIERRE + ".");
        }
    }

    /**
     * Reglas que protegen la agenda de un uso abusivo: una persona no puede estar en dos sesiones a la vez, no puede
     * retener cupos sin pagar y quien deja expirar reservas repetidamente queda bloqueado un tiempo.
     */
    private void validarCliente(Reserva reserva, Long excluirId, boolean nueva, LocalDateTime ahora) {
        Long clienteId = reserva.getCliente().getId();
        LocalDateTime inicio = reserva.getFechaHora();
        List<Reserva> propias = reservaRepository.buscarConflictosCliente(clienteId, inicio,
                ReglasReserva.finSesion(reserva), excluirId);
        if (!propias.isEmpty()) {
            throw new ReservaConflictException("El cliente ya tiene otra reserva que se cruza con ese horario: "
                    + "una persona no puede estar en dos sesiones a la vez.");
        }
        if (nueva && reserva.getEstado() == EstadoReserva.PENDIENTE) {
            if (reservaRepository.countByClienteIdAndEstadoAndCreadaEnIsNotNull(clienteId, EstadoReserva.PENDIENTE) >= ReglasReserva.MAX_PENDIENTES_CLIENTE) {
                throw new ReservaConflictException("Ya tienes una reserva pendiente de pago. Págala (tienes "
                        + ReglasReserva.HOLD_PAGO_MIN + " min desde que la creaste) o cancélala antes de reservar otro turno.");
            }
            long expiradas = reservaRepository.countByClienteIdAndObservacionAndCanceladaEnGreaterThanEqual(clienteId,
                    ReglasReserva.EXPIRADA_SIN_PAGO, ahora.minusHours(ReglasReserva.VENTANA_EXPIRADAS_HORAS));
            if (expiradas >= ReglasReserva.MAX_EXPIRADAS_VENTANA) {
                throw new ReservaBloqueadaException("Dejaste vencer " + expiradas + " reservas sin pagar en las últimas "
                        + ReglasReserva.VENTANA_EXPIRADAS_HORAS + " horas. Por seguridad de la agenda, reserva de nuevo mañana o pide a recepción que lo haga en el mostrador.");
            }
        }
    }

    /** Libera el cupo de las reservas PENDIENTES cuyo plazo de pago ya venció (no esperan al planificador). */
    void liberarPlazosVencidos(LocalDateTime ahora) {
        for (Reserva r : reservaRepository.buscarPendientesCreadasHasta(ahora.minusMinutes(ReglasReserva.HOLD_PAGO_MIN))) {
            r.setEstado(EstadoReserva.CANCELADA);
            r.setCanceladaEn(ahora);
            r.setObservacion(ReglasReserva.EXPIRADA_SIN_PAGO);
            reservaRepository.save(r);
        }
    }

    /**
     * Regla de negocio central (misma que el Core Engine del APF1): ningún especialista ni sala
     * puede tener dos reservas activas cuyos bloques [inicio, inicio + duración + limpieza) se crucen.
     * Se evalúa con los recursos ya bloqueados, así que la consulta ve todo lo que otras reservas confirmaron.
     */
    private void validarDisponibilidad(Reserva reserva, Long excluirId) {
        exigirDatosCompletos(reserva);
        LocalDateTime inicio = reserva.getFechaHora();
        LocalDateTime fin = ReservaResponse.finBloque(inicio, reserva.getServicio());
        if (!reserva.getSala().isActiva()) {
            throw new ReservaConflictException(reserva.getSala().getNombre() + " está en mantenimiento y no recibe reservas.");
        }
        if (!ausenciaRepository.buscarCruces(reserva.getEmpleado().getId(), inicio, fin).isEmpty()) {
            throw new ReservaConflictException(reserva.getEmpleado().getNombre() + " no atiende en ese horario (ausencia programada).");
        }
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
        reserva.setEmpleado(empleadoRepository.buscarBloqueado(request.empleadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", request.empleadoId())));
        if (!reserva.getEmpleado().isActivo()) {
            throw new IllegalArgumentException(reserva.getEmpleado().getNombre() + " está dada de baja y no recibe reservas");
        }
        reserva.setServicio(servicioRepository.findById(request.servicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio", request.servicioId())));
        reserva.setFechaHora(request.fechaHora());
        if (request.salaId() != null) {
            reserva.setSala(salaRepository.buscarBloqueada(request.salaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sala", request.salaId())));
        } else if (reserva.getSala() == null) {
            reserva.setSala(asignarSalaLibre(reserva));
        } else {
            salaRepository.buscarBloqueada(reserva.getSala().getId());
        }
    }

    /**
     * Sin sala indicada se usa la primera cabina libre durante todo el bloque (duración + limpieza). Cada candidata
     * se bloquea y se vuelve a comprobar: si otra reserva la tomó mientras tanto, se pasa a la siguiente.
     */
    private Sala asignarSalaLibre(Reserva reserva) {
        if (reserva.getFechaHora() == null) {
            throw new IllegalArgumentException("Debe indicar la fecha y hora de la reserva");
        }
        LocalDateTime fin = ReservaResponse.finBloque(reserva.getFechaHora(), reserva.getServicio());
        for (Sala candidata : salaRepository.buscarDisponibles(reserva.getFechaHora(), fin)) {
            Sala bloqueada = salaRepository.buscarBloqueada(candidata.getId()).orElse(null);
            if (bloqueada != null && reservaRepository.buscarConflictos(bloqueada.getId(), -1L,
                    reserva.getFechaHora(), fin, reserva.getId()).isEmpty()) {
                return bloqueada;
            }
        }
        throw new ReservaConflictException("No hay cabinas disponibles en el turno solicitado.");
    }

    private boolean estaActiva(Reserva r) {
        return r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.CONFIRMADA;
    }

    static void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }

    /**
     * Turnos del día con su disponibilidad para esa terapeuta y servicio, así la pantalla de reserva solo ofrece lo que
     * realmente se puede tomar (y explica por qué el resto no). Es informativo: la reserva vuelve a validar todo con
     * los recursos bloqueados, porque entre mirar y reservar otra persona puede tomar el turno.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<TurnoDisponibleResponse> disponibilidad(Long empleadoId, Long servicioId, LocalDate fecha, String correoCliente) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", empleadoId));
        var servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio", servicioId));
        Long clienteId = correoCliente != null ? clienteIdDelUsuario(correoCliente) : null;
        LocalDateTime ahora = LocalDateTime.now();
        liberarPlazosVencidos(ahora);
        List<TurnoDisponibleResponse> turnos = new ArrayList<>();
        for (LocalTime turno : ReglasReserva.TURNOS) {
            LocalDateTime inicio = fecha.atTime(turno);
            LocalDateTime finSesion = inicio.plusMinutes(servicio.getDuracionMinutos() != null ? servicio.getDuracionMinutos() : 60);
            LocalDateTime finBloque = ReservaResponse.finBloque(inicio, servicio);
            String motivo = null;
            if (!empleado.isActivo()) {
                motivo = empleado.getNombre() + " no está atendiendo";
            } else if (inicio.isBefore(ahora)) {
                motivo = "Este horario ya pasó";
            } else if (!ReglasReserva.dentroDeHorario(inicio, finSesion)) {
                motivo = "La sesión terminaría fuera del horario de atención";
            } else if (!ausenciaRepository.buscarCruces(empleadoId, inicio, finBloque).isEmpty()) {
                motivo = empleado.getNombre() + " no atiende ese día";
            } else if (!reservaRepository.buscarConflictos(-1L, empleadoId, inicio, finBloque, null).isEmpty()) {
                motivo = empleado.getNombre() + " ya tiene una sesión a esa hora";
            } else if (salaRepository.buscarDisponibles(inicio, finBloque).isEmpty()) {
                motivo = "No quedan cabinas libres a esa hora";
            } else if (clienteId != null && !reservaRepository.buscarConflictosCliente(clienteId, inicio, finSesion, null).isEmpty()) {
                motivo = "Ya tienes otra reserva a esa hora";
            }
            turnos.add(new TurnoDisponibleResponse(turno.toString(), motivo == null, motivo));
        }
        return turnos;
    }

    private Long clienteIdDelUsuario(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("El usuario no tiene un cliente asociado"))
                .getId();
    }
}
