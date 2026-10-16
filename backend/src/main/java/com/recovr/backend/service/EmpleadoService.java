package com.recovr.backend.service;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpleadoService {

    private static final java.util.List<EstadoReserva> RESERVAS_ACTIVAS =
            java.util.List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    private final EmpleadoRepository empleadoRepository;
    private final ReservaRepository reservaRepository;
    private final com.recovr.backend.repository.AusenciaEmpleadoRepository ausenciaRepository;
    private final com.recovr.backend.repository.FotoEmpleadoRepository fotoRepository;
    private final com.recovr.backend.repository.UsuarioRepository usuarioRepository;

    public EmpleadoService(EmpleadoRepository empleadoRepository, ReservaRepository reservaRepository,
                           com.recovr.backend.repository.AusenciaEmpleadoRepository ausenciaRepository,
                           com.recovr.backend.repository.FotoEmpleadoRepository fotoRepository,
                           com.recovr.backend.repository.UsuarioRepository usuarioRepository) {
        this.empleadoRepository = empleadoRepository;
        this.reservaRepository = reservaRepository;
        this.ausenciaRepository = ausenciaRepository;
        this.fotoRepository = fotoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<com.recovr.backend.entity.AusenciaEmpleado> ausencias(Long empleadoId) {
        buscarPorId(empleadoId);
        return ausenciaRepository.findByEmpleadoIdOrderByDesdeAsc(empleadoId);
    }

    /**
     * Registra una ausencia (vacaciones, licencia). Mientras dura no recibe reservas; si ya tiene reservas activas
     * en ese periodo no se puede registrar: primero se cancelan o se reasignan, igual que con la baja.
     */
    @org.springframework.transaction.annotation.Transactional
    public com.recovr.backend.entity.AusenciaEmpleado registrarAusencia(Long empleadoId, LocalDateTime desde,
                                                                         LocalDateTime hasta, String motivo) {
        Empleado empleado = buscarPorId(empleadoId);
        ReservaService.validarRango(desde, hasta);
        if (!reservaRepository.buscarConflictos(-1L, empleadoId, desde, hasta, null).isEmpty()) {
            throw new IllegalStateException("No se puede registrar la ausencia de " + empleado.getNombre()
                    + ": tiene reservas activas en ese periodo. Cancélalas o reasígnalas primero.");
        }
        com.recovr.backend.entity.AusenciaEmpleado a = new com.recovr.backend.entity.AusenciaEmpleado();
        a.setEmpleado(empleado);
        a.setDesde(desde);
        a.setHasta(hasta);
        a.setMotivo(motivo);
        return ausenciaRepository.save(a);
    }

    public void eliminarAusencia(Long ausenciaId) {
        if (!ausenciaRepository.existsById(ausenciaId)) {
            throw new RecursoNoEncontradoException("Ausencia", ausenciaId);
        }
        ausenciaRepository.deleteById(ausenciaId);
    }

    public List<Empleado> listarTodos() {
        return empleadoRepository.findAll();
    }

    /** Terapeutas que reciben reservas (las dadas de baja no aparecen al reservar). */
    public List<Empleado> listarActivos() {
        return empleadoRepository.findAll().stream().filter(Empleado::isActivo).toList();
    }

    /**
     * Baja lógica: la terapeuta conserva su historial pero deja de recibir reservas. No se puede dar de baja
     * mientras tenga reservas activas (PENDIENTE o CONFIRMADA): primero se cancelan o se reasignan.
     */
    public Empleado cambiarActivo(Long id, boolean activo) {
        Empleado empleado = buscarPorId(id);
        if (!activo && reservaRepository.existsByEmpleadoIdAndEstadoIn(id, RESERVAS_ACTIVAS)) {
            throw new IllegalStateException("No se puede dar de baja a " + empleado.getNombre()
                    + ": tiene reservas activas (PENDIENTE o CONFIRMADA). Cancélalas o reasígnalas primero.");
        }
        empleado.setActivo(activo);
        return empleadoRepository.save(empleado);
    }

    public Empleado buscarPorId(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado", id));
    }

    public Empleado crear(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }

    public Empleado actualizar(Long id, Empleado datos) {
        Empleado empleado = buscarPorId(id);
        empleado.setNombre(datos.getNombre());
        empleado.setEspecialidad(datos.getEspecialidad());
        empleado.setTelefono(datos.getTelefono());
        empleado.setBio(datos.getBio());
        return empleadoRepository.save(empleado);
    }

    @org.springframework.transaction.annotation.Transactional
    public void eliminar(Long id) {
        if (!empleadoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Empleado", id);
        }
        if (reservaRepository.existsByEmpleadoIdAndEstadoIn(id, RESERVAS_ACTIVAS)) {
            throw new IllegalStateException("No se puede eliminar al empleado: tiene reservas activas (PENDIENTE o CONFIRMADA)");
        }
        if (reservaRepository.existsByEmpleadoId(id)) {
            throw new IllegalStateException("No se puede eliminar al empleado: tiene historial de reservas. Dala de baja para que deje de recibir reservas conservando su historial.");
        }
        // No dejar huérfanos: la foto se borra con la terapeuta y su acceso (si lo tenía) se desactiva,
        // porque una cuenta ESPECIALISTA sin ficha vinculada solo produciría errores.
        Empleado empleado = buscarPorId(id);
        if (empleado.getUsuarioId() != null) {
            usuarioRepository.findById(empleado.getUsuarioId()).ifPresent(u -> {
                u.setActivo(false);
                usuarioRepository.save(u);
            });
        }
        ausenciaRepository.deleteAll(ausenciaRepository.findByEmpleadoIdOrderByDesdeAsc(id));
        fotoRepository.deleteById(id);
        empleadoRepository.deleteById(id);
    }

    public List<Empleado> buscarDisponibles(LocalDateTime inicio, LocalDateTime fin) {
        ReservaService.validarRango(inicio, fin);
        return empleadoRepository.buscarDisponibles(inicio, fin);
    }
}
