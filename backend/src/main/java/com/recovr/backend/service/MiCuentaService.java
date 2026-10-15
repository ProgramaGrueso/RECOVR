package com.recovr.backend.service;

import com.recovr.backend.dto.MiCuentaDtos.*;
import com.recovr.backend.dto.ReservaResponse;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Portal del cliente. Todo se calcula desde sus reservas reales:
 * <ul>
 *   <li>Puntos: 1 punto por cada S/ 1 de sesiones COMPLETADAS (no se regalan ni se editan).</li>
 *   <li>Nivel: Bronce (0), Plata (500), Oro (1500) y Diamante (3000 puntos).</li>
 *   <li>Cancelaciones: las hechas por el propio cliente en el mes, contra el tope de la política.</li>
 * </ul>
 */
@Service
public class MiCuentaService {

    static final List<NivelInfo> NIVELES = List.of(
            new NivelInfo("Bronce", 0), new NivelInfo("Plata", 500), new NivelInfo("Oro", 1500), new NivelInfo("Diamante", 3000));

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    public MiCuentaService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                           ReservaRepository reservaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public MiCuentaResponse resumen(String correo) {
        return resumen(correo, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public MiCuentaResponse resumen(String correo, LocalDateTime ahora) {
        Cliente cliente = clienteDe(correo);
        List<Reserva> reservas = reservaRepository.buscarPorCliente(cliente.getId());
        List<Reserva> completadas = reservas.stream().filter(r -> r.getEstado() == EstadoReserva.COMPLETADA).toList();
        int puntos = completadas.stream().mapToInt(r -> r.getServicio().getPrecio().intValue()).sum();

        NivelInfo actual = NIVELES.get(0);
        NivelInfo siguiente = null;
        for (int i = 0; i < NIVELES.size(); i++) {
            if (puntos >= NIVELES.get(i).desde()) {
                actual = NIVELES.get(i);
                siguiente = i + 1 < NIVELES.size() ? NIVELES.get(i + 1) : null;
            }
        }
        ReservaResponse proxima = reservas.stream()
                .filter(r -> ReglasReserva.activa(r) && !ReglasReserva.finSesion(r).isBefore(ahora))
                .min(Comparator.comparing(Reserva::getFechaHora)).map(ReservaResponse::de).orElse(null);
        long cancelaciones = reservaRepository.countByClienteIdAndObservacionAndCanceladaEnGreaterThanEqual(
                cliente.getId(), ReglasReserva.CANCELADA_POR_CLIENTE, ahora.toLocalDate().withDayOfMonth(1).atStartOfDay());

        return new MiCuentaResponse(cliente.getNombre(), cliente.getCorreo(), cliente.getTelefono(), puntos, actual.nivel(),
                siguiente != null ? siguiente.nivel() : null, siguiente != null ? siguiente.desde() - puntos : 0,
                completadas.size(), proxima, cancelaciones, ReglasReserva.MAX_CANCELACIONES_CLIENTE_MES,
                ReglasReserva.HORAS_MIN_CANCELACION_CLIENTE, ahora);
    }

    @Transactional
    public MiCuentaResponse actualizar(String correo, ActualizarPerfilRequest request) {
        Cliente cliente = clienteDe(correo);
        cliente.setNombre(request.nombre().trim());
        cliente.setTelefono(request.telefono());
        clienteRepository.save(cliente);
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseThrow();
        usuario.setNombre(cliente.getNombre());
        usuarioRepository.save(usuario);
        return resumen(correo);
    }

    private Cliente clienteDe(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("El usuario no tiene un cliente asociado"));
    }
}
