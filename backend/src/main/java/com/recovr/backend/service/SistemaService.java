package com.recovr.backend.service;

import com.recovr.backend.dto.SistemaDtos.*;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.repository.*;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estado real del sistema para el administrador: base de datos, memoria, volúmenes, ciclo de vida y seguridad. */
@Service
public class SistemaService {

    private final DataSource dataSource;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServicioRepository servicioRepository;
    private final SalaRepository salaRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final ReservaCicloVidaService cicloVida;
    private final AuditoriaService auditoriaService;

    public SistemaService(DataSource dataSource, ClienteRepository clienteRepository, EmpleadoRepository empleadoRepository,
                          ServicioRepository servicioRepository, SalaRepository salaRepository, ReservaRepository reservaRepository,
                          PagoRepository pagoRepository, UsuarioRepository usuarioRepository, AuditoriaRepository auditoriaRepository,
                          ReservaCicloVidaService cicloVida, AuditoriaService auditoriaService) {
        this.dataSource = dataSource;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.servicioRepository = servicioRepository;
        this.salaRepository = salaRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.cicloVida = cicloVida;
        this.auditoriaService = auditoriaService;
    }

    public List<AuditoriaResponse> auditoria(int limite) {
        return auditoriaService.ultimos(limite).stream()
                .map(a -> new AuditoriaResponse(a.getId(), a.getFecha(), a.getUsuario(), a.getRol(), a.getAccion(),
                        a.getRecurso(), a.getEstado(), a.getIp(), a.getDetalle()))
                .toList();
    }

    public EstadoSistemaResponse estado() {
        boolean dbOk = false;
        String dbNombre = "sin conexión";
        try (Connection c = dataSource.getConnection()) {
            dbOk = c.isValid(2);
            dbNombre = c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion();
        } catch (Exception ignored) {
            // se reporta como inactiva
        }
        Runtime rt = Runtime.getRuntime();
        long inicio = ManagementFactory.getRuntimeMXBean().getStartTime();

        Map<String, Long> registros = new LinkedHashMap<>();
        registros.put("usuarios", usuarioRepository.count());
        registros.put("clientes", clienteRepository.count());
        registros.put("terapeutas", empleadoRepository.count());
        registros.put("servicios", servicioRepository.count());
        registros.put("cabinas", salaRepository.count());
        registros.put("reservas", reservaRepository.count());
        registros.put("pagos", pagoRepository.count());
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoReserva e : EstadoReserva.values()) {
            porEstado.put(e.name(), reservaRepository.countByEstado(e));
        }
        Map<String, Long> porRol = new LinkedHashMap<>();
        for (Rol r : Rol.values()) {
            porRol.put(r.name(), usuarioRepository.countByRol(r));
        }
        long denegados = auditoriaService.ultimos(500).stream()
                .filter(a -> a.getFecha().isAfter(LocalDateTime.now().minusHours(24)))
                .filter(a -> (a.getEstado() != null && (a.getEstado() == 401 || a.getEstado() == 403 || a.getEstado() == 429))
                        || "LOGIN_FALLIDO".equals(a.getAccion()) || "LOGIN_BLOQUEADO".equals(a.getAccion()))
                .count();

        return new EstadoSistemaResponse("RECOVR backend 1.0", System.getProperty("java.version"), SpringBootVersion.getVersion(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(inicio), ZoneId.systemDefault()),
                (System.currentTimeMillis() - inicio) / 1000, dbOk, dbNombre,
                (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024), rt.maxMemory() / (1024 * 1024),
                registros, porEstado, porRol,
                cicloVida.getUltimaEjecucion(), cicloVida.getUltimosCambios(), cicloVida.getTotalCambios(), denegados);
    }
}
