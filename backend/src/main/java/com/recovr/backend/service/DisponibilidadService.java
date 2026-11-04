package com.recovr.backend.service;

import com.recovr.backend.dto.TurnoDisponibleResponse;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import com.recovr.backend.repository.AusenciaEmpleadoRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Consulta (solo informativa) de los turnos que se pueden reservar con una terapeuta y un servicio en una fecha. */
@Service
public class DisponibilidadService {

    private final EmpleadoRepository empleadoRepository;
    private final ServicioRepository servicioRepository;
    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final AusenciaEmpleadoRepository ausenciaRepository;
    private final ReservaCicloVidaService cicloVida;
    private final UsuarioActualService usuarioActual;
    private final Clock reloj;

    public DisponibilidadService(EmpleadoRepository empleadoRepository, ServicioRepository servicioRepository,
                                 ReservaRepository reservaRepository, SalaRepository salaRepository,
                                 AusenciaEmpleadoRepository ausenciaRepository, ReservaCicloVidaService cicloVida,
                                 UsuarioActualService usuarioActual, Clock reloj) {
        this.empleadoRepository = empleadoRepository;
        this.servicioRepository = servicioRepository;
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.ausenciaRepository = ausenciaRepository;
        this.cicloVida = cicloVida;
        this.usuarioActual = usuarioActual;
        this.reloj = reloj;
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
        Long clienteId = correoCliente != null ? usuarioActual.clienteId(correoCliente) : null;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        cicloVida.expirarPlazosVencidos(ahora);
        List<TurnoDisponibleResponse> turnos = new ArrayList<>();
        for (LocalTime turno : ReglasReserva.TURNOS) {
            LocalDateTime inicio = fecha.atTime(turno);
            LocalDateTime finSesion = inicio.plusMinutes(servicio.getDuracionMinutos() != null ? servicio.getDuracionMinutos() : 60);
            LocalDateTime finBloque = ReglasReserva.finBloque(inicio, servicio);
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
}
