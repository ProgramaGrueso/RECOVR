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
            "El descuento nunca supera el precio de la sesión y no se puede combinar con otro código.");

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

    /** Puntos de una sesión: lo pagado (o el precio si aún no hay pago registrado), redondeado hacia abajo. */
    static int puntosDe(Reserva r) {
        BigDecimal base = r.getPago() != null && r.getPago().getMonto() != null ? r.getPago().getMonto() : r.getServicio().getPrecio();
        return base.intValue();
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
        int canjeados = canjes.stream().mapToInt(Canje::getPuntos).sum();
        int saldo = ganados - canjeados;

        List<Movimiento> movimientos = new ArrayList<>();
        completadas.forEach(r -> movimientos.add(new Movimiento(r.getFechaHora(), "GANADO",
                "Sesión completada: " + r.getServicio().getNombre(), puntosDe(r))));
        canjes.forEach(c -> movimientos.add(new Movimiento(c.getCreadoEn(), "CANJE", "Canje: " + c.getTitulo() + " (" + c.getCodigo() + ")", -c.getPuntos())));
        movimientos.sort(Comparator.comparing(Movimiento::fecha).reversed());

        Nivel actual = nivelPara(ganados);
        Nivel siguiente = siguienteNivel(ganados);
        return new PuntosResponse(saldo, ganados, canjeados, actual.nombre(),
                siguiente != null ? siguiente.nombre() : null, siguiente != null ? siguiente.desde() - ganados : 0,
                REGLAS,
                NIVELES.stream().map(n -> new NivelPuntos(n.nombre(), n.desde(), n.equals(actual))).toList(),
                BENEFICIOS.stream().map(b -> new BeneficioInfo(b.id(), b.titulo(), b.descripcion(), b.puntos(), b.descuento(), saldo >= b.puntos())).toList(),
                movimientos,
                canjes.stream().map(c -> new CanjeInfo(c.getCodigo(), c.getTitulo(), c.getDescuento(), c.getEstado().name(), c.getCreadoEn(), c.getUsadoEn())).toList());
    }

    @Transactional
    public CanjeInfo canjear(String correo, String beneficioId) {
        Cliente cliente = clienteDe(correo);
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
        c = canjeRepository.save(c);
        return new CanjeInfo(c.getCodigo(), c.getTitulo(), c.getDescuento(), c.getEstado().name(), c.getCreadoEn(), null);
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
    @Transactional(readOnly = true)
    public Canje canjeParaCobro(String codigo, Cliente clienteDeLaReserva) {
        Canje c = canjeRepository.findByCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Código de canje no encontrado: " + codigo));
        String motivo = motivoInvalido(c, clienteDeLaReserva);
        if (motivo != null) {
            throw new IllegalStateException(motivo);
        }
        return c;
    }

    private static String motivoInvalido(Canje c, Cliente cliente) {
        if (c.getEstado() != Canje.Estado.DISPONIBLE) {
            return "El código ya fue usado";
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
