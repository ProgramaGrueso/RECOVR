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
 * Portal del cliente. Todo se calcula desde sus reservas y canjes reales; las reglas de puntos están en {@link PuntosService}.
 * Las cancelaciones propias del mes se cuentan contra el tope de la política.
 */
@Service
public class MiCuentaService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final PuntosService puntosService;

    public MiCuentaService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                           ReservaRepository reservaRepository, PuntosService puntosService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
        this.puntosService = puntosService;
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
        int ganados = puntosService.ganados(cliente.getId());
        int saldo = puntosService.saldo(cliente.getId());
        PuntosService.Nivel actual = PuntosService.nivelPara(ganados);
        PuntosService.Nivel siguiente = PuntosService.siguienteNivel(ganados);
        ReservaResponse proxima = reservas.stream()
                .filter(r -> ReglasReserva.activa(r) && !ReglasReserva.finSesion(r).isBefore(ahora))
                .min(Comparator.comparing(Reserva::getFechaHora)).map(ReservaResponse::de).orElse(null);
        long cancelaciones = reservaRepository.countByClienteIdAndObservacionAndCanceladaEnGreaterThanEqual(
                cliente.getId(), ReglasReserva.CANCELADA_POR_CLIENTE, ahora.toLocalDate().withDayOfMonth(1).atStartOfDay());

        return new MiCuentaResponse(cliente.getNombre(), cliente.getCorreo(), cliente.getTelefono(), saldo, actual.nombre(),
                siguiente != null ? siguiente.nombre() : null, siguiente != null ? siguiente.desde() - ganados : 0,
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
