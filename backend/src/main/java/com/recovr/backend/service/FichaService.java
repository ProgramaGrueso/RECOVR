package com.recovr.backend.service;

import com.recovr.backend.dto.FichaDtos.*;
import com.recovr.backend.entity.*;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ficha confidencial de clientes. Principio de mínimo privilegio: solo una ESPECIALISTA puede verla o escribirla, y
 * únicamente de clientes con los que tiene (o tuvo) al menos una reserva. Ni ADMIN ni RECEPCIONISTA acceden a ella.
 */
@Service
public class FichaService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final FichaClienteRepository fichaRepository;
    private final NotaSesionRepository notaRepository;

    public FichaService(UsuarioRepository usuarioRepository, EmpleadoRepository empleadoRepository,
                        ClienteRepository clienteRepository, ReservaRepository reservaRepository,
                        FichaClienteRepository fichaRepository, NotaSesionRepository notaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
        this.fichaRepository = fichaRepository;
        this.notaRepository = notaRepository;
    }

    @Transactional(readOnly = true)
    public List<MiClienteResponse> misClientes(String correo) {
        Empleado yo = empleadoActual(correo);
        LocalDateTime ahora = LocalDateTime.now();
        Map<Long, List<Reserva>> porCliente = reservaRepository.buscarPorEmpleado(yo.getId()).stream()
                .collect(Collectors.groupingBy(r -> r.getCliente().getId()));
        return porCliente.values().stream().map(lista -> {
            Cliente c = lista.get(0).getCliente();
            LocalDateTime ultima = lista.stream().map(Reserva::getFechaHora).filter(f -> f.isBefore(ahora))
                    .max(Comparator.naturalOrder()).orElse(null);
            LocalDateTime proxima = lista.stream().filter(ReglasReserva::activa).map(Reserva::getFechaHora)
                    .filter(f -> !f.isBefore(ahora)).min(Comparator.naturalOrder()).orElse(null);
            return new MiClienteResponse(c.getId(), c.getNombre(), c.getTelefono(), lista.size(), ultima, proxima,
                    fichaRepository.findByClienteId(c.getId()).isPresent());
        }).sorted(Comparator.comparing(MiClienteResponse::nombre)).toList();
    }

    @Transactional(readOnly = true)
    public FichaResponse obtener(String correo, Long clienteId) {
        Empleado yo = autorizar(correo, clienteId);
        return respuesta(clienteRepository.findById(clienteId).orElseThrow(), yo);
    }

    @Transactional
    public FichaResponse guardar(String correo, Long clienteId, FichaRequest req) {
        Empleado yo = autorizar(correo, clienteId);
        Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
        FichaCliente ficha = fichaRepository.findByClienteId(clienteId).orElseGet(() -> {
            FichaCliente nueva = new FichaCliente();
            nueva.setCliente(cliente);
            return nueva;
        });
        ficha.setAlergias(req.alergias());
        ficha.setCondiciones(req.condiciones());
        ficha.setPresionPreferida(req.presionPreferida());
        ficha.setAceites(req.aceites());
        ficha.setZonasRestringidas(req.zonasRestringidas());
        ficha.setNotas(req.notas());
        ficha.setActualizadoPor(yo.getNombre());
        ficha.setActualizadoEn(LocalDateTime.now());
        fichaRepository.save(ficha);
        return respuesta(cliente, yo);
    }

    @Transactional
    public FichaResponse agregarNota(String correo, Long clienteId, NotaRequest req) {
        Empleado yo = autorizar(correo, clienteId);
        Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
        NotaSesion nota = new NotaSesion();
        nota.setCliente(cliente);
        nota.setEmpleado(yo);
        nota.setFecha(LocalDateTime.now());
        nota.setTratamiento(req.tratamiento().trim());
        nota.setObservaciones(req.observaciones());
        nota.setRecomendaciones(req.recomendaciones());
        if (req.reservaId() != null) {
            Reserva r = reservaRepository.findById(req.reservaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", req.reservaId()));
            if (!r.getCliente().getId().equals(clienteId) || !r.getEmpleado().getId().equals(yo.getId())) {
                throw new IllegalArgumentException("La sesión indicada no es tuya o no corresponde a este cliente");
            }
            nota.setReserva(r);
        }
        notaRepository.save(nota);
        return respuesta(cliente, yo);
    }

    @Transactional
    public void eliminarNota(String correo, Long notaId) {
        Empleado yo = empleadoActual(correo);
        NotaSesion nota = notaRepository.findById(notaId).orElseThrow(() -> new RecursoNoEncontradoException("Nota", notaId));
        if (!nota.getEmpleado().getId().equals(yo.getId())) {
            throw new AccessDeniedException("Solo puedes eliminar tus propias notas");
        }
        notaRepository.delete(nota);
    }

    private FichaResponse respuesta(Cliente c, Empleado yo) {
        FichaCliente f = fichaRepository.findByClienteId(c.getId()).orElse(null);
        List<NotaResponse> notas = notaRepository.findByClienteIdOrderByFechaDesc(c.getId()).stream()
                .map(n -> new NotaResponse(n.getId(), n.getFecha(), n.getEmpleado().getNombre(), n.getTratamiento(),
                        n.getObservaciones(), n.getRecomendaciones(), n.getReserva() != null ? n.getReserva().getId() : null,
                        n.getEmpleado().getId().equals(yo.getId())))
                .toList();
        return new FichaResponse(c.getId(), c.getNombre(), c.getTelefono(),
                f != null ? f.getAlergias() : null, f != null ? f.getCondiciones() : null,
                f != null ? f.getPresionPreferida() : null, f != null ? f.getAceites() : null,
                f != null ? f.getZonasRestringidas() : null, f != null ? f.getNotas() : null,
                f != null ? f.getActualizadoPor() : null, f != null ? f.getActualizadoEn() : null, notas);
    }

    private Empleado autorizar(String correo, Long clienteId) {
        Empleado yo = empleadoActual(correo);
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNoEncontradoException("Cliente", clienteId);
        }
        if (!reservaRepository.existsByEmpleadoIdAndClienteId(yo.getId(), clienteId)) {
            throw new AccessDeniedException("Solo puedes ver las fichas de clientes que atiendes");
        }
        return yo;
    }

    private Empleado empleadoActual(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return empleadoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Tu usuario no está vinculado a ninguna terapeuta"));
    }
}
