package com.recovr.backend.service;

import com.recovr.backend.dto.PuntosDtos.*;
import com.recovr.backend.entity.Canje;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.CanjeRepository;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Programa de puntos. Reglas (se muestran tal cual al cliente):
 * <ol>
 *   <li>Se gana 1 punto por cada S/ 1 pagado en una sesión COMPLETADA. Canceladas, expiradas y no asistidas no suman.</li>
 *   <li>Saldo = puntos ganados − puntos canjeados. El nivel se calcula con los puntos ganados en total, por eso
 *       canjear no te hace bajar de nivel.</li>
 *   <li>Canjear genera un código de un solo uso. Recepción lo aplica al cobrar una sesión del mismo cliente y el monto
 *       baja automáticamente; el código se consume al cobrar.</li>
 * </ol>
 */
@Service
public class PuntosService {

    public record Beneficio(String id, String titulo, String descripcion, int puntos, BigDecimal descuento) {}

    public record Nivel(String nombre, int desde) {}

    static final List<Nivel> NIVELES = List.of(new Nivel("Bronce", 0), new Nivel("Plata", 500), new Nivel("Oro", 1500), new Nivel("Diamante", 3000));

    static final List<Beneficio> BENEFICIOS = List.of(
            new Beneficio("descuento-30", "Descuento de S/ 30", "S/ 30 menos en cualquier sesión", 300, new BigDecimal("30")),
            new Beneficio("descuento-80", "Descuento de S/ 80", "S/ 80 menos en cualquier sesión", 700, new BigDecimal("80")),
            new Beneficio("sesion-relax", "Sesión Relajante de cortesía", "Cubre una sesión Relajante & Descontracturante (S/ 180)", 1200, new BigDecimal("180")));

    static final List<String> REGLAS = List.of(
            "Ganas 1 punto por cada S/ 1 que pagas en una sesión completada.",
            "Las sesiones canceladas, expiradas o a las que no asistes no suman puntos.",
            "Tu saldo es lo ganado menos lo canjeado. Tu nivel depende de lo ganado en total, así que canjear no te baja de nivel.",
            "Al canjear obtienes un código de un solo uso. Recepción lo aplica al cobrar tu sesión y el monto baja automáticamente.",
            "El descuento nunca supera el precio de la sesión y no se puede combinar con otro código.",
            "Los códigos canjeados con puntos vencen a los 90 días; puedes anular uno sin usar y recuperar tus puntos. Si una sesión pagada se cancela, recibes un crédito por lo pagado (vence a los 180 días).");

    private static final String CODIGO_ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final CanjeRepository canjeRepository;

    public PuntosService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                         ReservaRepository reservaRepository, CanjeRepository canjeRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
        this.canjeRepository = canjeRepository;
    }

    /** Puntos de una sesión: solo lo efectivamente pagado, redondeado hacia abajo. Sin pago no hay puntos. */
    static int puntosDe(Reserva r) {
        return r.getPago() != null && r.getPago().getMonto() != null ? r.getPago().getMonto().intValue() : 0;
    }

    @Transactional(readOnly = true)
    public int ganados(Long clienteId) {
        return reservaRepository.buscarPorCliente(clienteId).stream()
                .filter(r -> r.getEstado() == EstadoReserva.COMPLETADA).mapToInt(PuntosService::puntosDe).sum();
    }

    @Transactional(readOnly = true)
    public int saldo(Long clienteId) {
        return ganados(clienteId) - (int) canjeRepository.sumarPuntosCanjeados(clienteId);
    }

    public static Nivel nivelPara(int ganados) {
        Nivel actual = NIVELES.get(0);
        for (Nivel n : NIVELES) {
            if (ganados >= n.desde()) {
                actual = n;
            }
        }
        return actual;
    }

    public static Nivel siguienteNivel(int ganados) {
        return NIVELES.stream().filter(n -> n.desde() > ganados).findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public PuntosResponse detalle(String correo) {
        Cliente cliente = clienteDe(correo);
        List<Reserva> completadas = reservaRepository.buscarPorCliente(cliente.getId()).stream()
                .filter(r -> r.getEstado() == EstadoReserva.COMPLETADA).toList();
        List<Canje> canjes = canjeRepository.findByClienteIdOrderByCreadoEnDesc(cliente.getId());
        int ganados = completadas.stream().mapToInt(PuntosService::puntosDe).sum();
        // Un código anulado devolvió sus puntos: no cuenta como canjeado
        int canjeados = canjes.stream().filter(c -> c.getEstado() != Canje.Estado.ANULADO).mapToInt(Canje::getPuntos).sum();
        int saldo = ganados - canjeados;

        List<Movimiento> movimientos = new ArrayList<>();
        completadas.forEach(r -> movimientos.add(new Movimiento(r.getFechaHora(), "GANADO",
                "Sesión completada: " + r.getServicio().getNombre(), puntosDe(r))));
        canjes.stream().filter(c -> c.getEstado() != Canje.Estado.ANULADO && c.getPuntos() > 0)
                .forEach(c -> movimientos.add(new Movimiento(c.getCreadoEn(), "CANJE", "Canje: " + c.getTitulo() + " (" + c.getCodigo() + ")", -c.getPuntos())));
        movimientos.sort(Comparator.comparing(Movimiento::fecha).reversed());

        Nivel actual = nivelPara(ganados);
        Nivel siguiente = siguienteNivel(ganados);
        return new PuntosResponse(saldo, ganados, canjeados, actual.nombre(),
                siguiente != null ? siguiente.nombre() : null, siguiente != null ? siguiente.desde() - ganados : 0,
                REGLAS,
                NIVELES.stream().map(n -> new NivelPuntos(n.nombre(), n.desde(), n.equals(actual))).toList(),
                BENEFICIOS.stream().map(b -> new BeneficioInfo(b.id(), b.titulo(), b.descripcion(), b.puntos(), b.descuento(), saldo >= b.puntos())).toList(),
                movimientos,
                canjes.stream().map(this::info).toList());
    }

    /**
     * Canjea puntos por un beneficio. Se bloquea la fila del cliente y se lee en READ_COMMITTED: dos canjes
     * simultáneos se hacen en fila y el segundo ve el saldo ya descontado (no se gasta dos veces el mismo saldo).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CanjeInfo canjear(String correo, String beneficioId) {
        Cliente cliente = clienteRepository.buscarBloqueado(clienteDe(correo).getId()).orElseThrow();
        Beneficio b = BENEFICIOS.stream().filter(x -> x.id().equals(beneficioId)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficio no encontrado: " + beneficioId));
        int saldo = saldo(cliente.getId());
        if (saldo < b.puntos()) {
            throw new IllegalStateException("Puntos insuficientes: tienes " + saldo + " y este beneficio cuesta " + b.puntos());
        }
        Canje c = new Canje();
        c.setCliente(cliente);
        c.setBeneficioId(b.id());
        c.setTitulo(b.titulo());
        c.setPuntos(b.puntos());
        c.setDescuento(b.descuento());
        c.setCodigo(codigoUnico());
        c.setCreadoEn(LocalDateTime.now());
        c.setVenceEn(c.getCreadoEn().plusDays(ReglasReserva.VIGENCIA_CANJE_DIAS));
        c = canjeRepository.save(c);
        return info(c);
    }

    private CanjeInfo info(Canje c) {
        boolean vencido = c.vencido(LocalDateTime.now());
        boolean anulable = c.getEstado() == Canje.Estado.DISPONIBLE && c.getPuntos() > 0;
        return new CanjeInfo(c.getCodigo(), c.getTitulo(), c.getDescuento(), vencido ? "VENCIDO" : c.getEstado().name(),
                c.getCreadoEn(), c.getUsadoEn(), c.getVenceEn(), anulable);
    }

    /**
     * El cliente anula un código que no usó y recupera los puntos. Los créditos por cancelación (que no costaron
     * puntos) no se anulan, y uno ya vencido no devuelve puntos: caducó sin usarse.
     */
    @Transactional
    public CanjeInfo anular(String correo, String codigo) {
        Cliente cliente = clienteDe(correo);
        Canje c = canjeRepository.buscarPorCodigoBloqueado(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Código de canje no encontrado: " + codigo));
        if (!c.getCliente().getId().equals(cliente.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Ese código no es tuyo");
        }
        if (c.getEstado() != Canje.Estado.DISPONIBLE) {
            throw new IllegalStateException("Solo se puede anular un código que no se haya usado");
        }
        if (c.getPuntos() <= 0) {
            throw new IllegalStateException("Un crédito por cancelación no se anula: se aplica al pagar tu próxima sesión");
        }
        if (c.vencido(LocalDateTime.now())) {
            throw new IllegalStateException("El código ya venció: los puntos de un código vencido no se devuelven");
        }
        c.setEstado(Canje.Estado.ANULADO);
        canjeRepository.save(c);
        return info(c);
    }

    /** Vista previa para recepción: monto que corresponde cobrar si se aplica el código a esa reserva. */
    @Transactional(readOnly = true)
    public CanjeValidoResponse vistaPrevia(String codigo, Reserva reserva) {
        Canje c = canjeRepository.findByCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Código de canje no encontrado: " + codigo));
        String motivo = motivoInvalido(c, reserva.getCliente());
        BigDecimal precio = reserva.getServicio().getPrecio();
        BigDecimal monto = precio.subtract(c.getDescuento()).max(BigDecimal.ZERO);
        return new CanjeValidoResponse(c.getCodigo(), c.getTitulo(), c.getCliente().getNombre(), c.getDescuento(), c.getEstado().name(),
                motivo == null, motivo, precio, monto);
    }

    /** Valida un código al cobrar. Falla con un motivo claro si no corresponde. */
    @Transactional
    public Canje canjeParaCobro(String codigo, Cliente clienteDeLaReserva) {
        Canje c = canjeRepository.buscarPorCodigoBloqueado(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Código de canje no encontrado: " + codigo));
        String motivo = motivoInvalido(c, clienteDeLaReserva);
        if (motivo != null) {
            throw new IllegalStateException(motivo);
        }
        return c;
    }

    /**
     * Crédito a favor del cliente por una sesión pagada que se canceló. Es un código de un solo uso (como un canje)
     * por el importe pagado, sin costo en puntos; recepción lo aplica al cobrar la siguiente sesión.
     */
    @Transactional
    public Canje crearCredito(Cliente cliente, BigDecimal importe, String titulo) {
        Canje c = new Canje();
        c.setCliente(cliente);
        c.setBeneficioId(ReglasReserva.CREDITO_CANCELACION);
        c.setTitulo(titulo);
        c.setPuntos(0);
        c.setDescuento(importe);
        c.setCodigo(codigoUnico());
        c.setCreadoEn(LocalDateTime.now());
        c.setVenceEn(c.getCreadoEn().plusDays(ReglasReserva.VIGENCIA_CREDITO_DIAS));
        return canjeRepository.save(c);
    }

    /** Si la reserva que se cancela había consumido un código, el código vuelve a quedar disponible. */
    @Transactional
    public void liberarCanjesDe(Reserva reserva) {
        for (Canje c : canjeRepository.findByReservaId(reserva.getId())) {
            if (c.getEstado() == Canje.Estado.USADO) {
                c.setEstado(Canje.Estado.DISPONIBLE);
                c.setUsadoEn(null);
                c.setReserva(null);
                canjeRepository.save(c);
            }
        }
    }

    private static String motivoInvalido(Canje c, Cliente cliente) {
        if (c.getEstado() == Canje.Estado.ANULADO) {
            return "El código fue anulado";
        }
        if (c.getEstado() != Canje.Estado.DISPONIBLE) {
            return "El código ya fue usado";
        }
        if (c.vencido(LocalDateTime.now())) {
            return "El código venció el " + c.getVenceEn().toLocalDate();
        }
        if (!c.getCliente().getId().equals(cliente.getId())) {
            return "El código pertenece a otro cliente";
        }
        return null;
    }

    private String codigoUnico() {
        for (int intento = 0; intento < 20; intento++) {
            StringBuilder sb = new StringBuilder("CJ-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODIGO_ALFABETO.charAt(RANDOM.nextInt(CODIGO_ALFABETO.length())));
            }
            if (canjeRepository.findByCodigo(sb.toString()).isEmpty()) {
                return sb.toString();
            }
        }
        throw new IllegalStateException("No se pudo generar un código único; inténtalo de nuevo");
    }

    private Cliente clienteDe(String correo) {
        var usuario = usuarioRepository.findByCorreo(correo).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("El usuario no tiene un cliente asociado"));
    }
}
