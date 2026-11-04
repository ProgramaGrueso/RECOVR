package com.recovr.backend.config;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.FichaCliente;
import com.recovr.backend.entity.NotaSesion;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.FichaClienteRepository;
import com.recovr.backend.repository.NotaSesionRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.service.ReglasReserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>Datos de demostración</b> (perfil {@code demo} o {@code recovr.seed.demo-data=true}): deja la base como si el
 * sistema llevara {@code recovr.seed.dias-historial} días en uso (90 por defecto) y con citas ya agendadas hasta el
 * 31 de diciembre (o {@code recovr.seed.fin}). La agenda la arma {@link GeneradorAgendaDemo}; los pacientes,
 * {@link PacientesDemo}. Todo es ficticio y queda marcado con el dominio {@value PacientesDemo#DOMINIO}.
 *
 * <p><b>Re-ejecutable sin duplicar:</b> los pacientes se buscan por correo y un día que ya tiene citas de pacientes
 * demo no se vuelve a sembrar. Las citas nuevas tampoco pisan reservas reales que ya existan (misma cabina o terapeuta).
 *
 * <p>Además crea las cuentas de acceso de demostración (administración, recepción, las 4 terapeutas y 5 clientes),
 * todas con la contraseña de DEMO_PASSWORD (variable de entorno; sin ella no se crean). SOLO para desarrollo local.
 */
@Component
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String DOMINIO = PacientesDemo.DOMINIO;
    /** Dominio de los datos demo de versiones anteriores (se reconoce para no volver a sembrar esos días). */
    static final String DOMINIO_ANTERIOR = "@recovr.test";
    static final int PACIENTES = 140;
    private static final String[] METODOS_PAGO = {"Yape", "Plin", "Efectivo", "Tarjeta"};

    private final CatalogoSeeder catalogo;
    private final boolean activo;
    private final int diasHistorial;
    private final String finConfigurado;
    private final String demoPassword;
    private final Clock reloj;
    private LocalDate inicio;
    private LocalDate fin;

    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaccion;
    private final FichaClienteRepository fichaRepository;
    private final NotaSesionRepository notaRepository;

    public DemoDataSeeder(CatalogoSeeder catalogo,
                          @Value("${recovr.seed.demo-data:false}") boolean activo,
                          @Value("${recovr.seed.dias-historial:90}") int diasHistorial,
                          @Value("${recovr.seed.fin:}") String finConfigurado,
                          @Value("${recovr.seed.demo-password:}") String demoPassword,
                          Clock reloj,
                          EmpleadoRepository empleadoRepository, ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                          PasswordEncoder passwordEncoder,
                          TransactionTemplate transaccion, FichaClienteRepository fichaRepository,
                          NotaSesionRepository notaRepository) {
        this.catalogo = catalogo;
        this.activo = activo;
        this.diasHistorial = diasHistorial;
        this.finConfigurado = finConfigurado == null ? "" : finConfigurado.trim();
        this.demoPassword = demoPassword == null ? "" : demoPassword;
        if (!this.demoPassword.isEmpty() && (this.demoPassword.length() < 10
                || !this.demoPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$"))) {
            throw new IllegalStateException("DEMO_PASSWORD debe tener al menos 10 caracteres, con letras y números.");
        }
        this.reloj = reloj;
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.passwordEncoder = passwordEncoder;
        this.transaccion = transaccion;
        this.fichaRepository = fichaRepository;
        this.notaRepository = notaRepository;
    }

    @Override
    public void run(String... args) {
        if (!activo) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        inicio = hoy.minusDays(diasHistorial);
        fin = finConfigurado.isEmpty() ? hoy.withMonth(12).withDayOfMonth(31) : LocalDate.parse(finConfigurado);
        if (fin.isBefore(hoy)) {
            fin = hoy;
        }
        int total = transaccion.execute(status -> sembrarAgenda(ahora));
        log.info("DemoDataSeeder — {} citas demo nuevas ({} → {}).", total, inicio, fin);
        // Cuentas de acceso: solo con DEMO_PASSWORD (idempotente; si cambia, se actualiza en todas).
        transaccion.executeWithoutResult(status -> {
            if (demoPassword.isEmpty()) {
                log.warn("DemoDataSeeder — DEMO_PASSWORD no está definida: no se crean cuentas de acceso demo.");
                desactivarCuentasDemo(true);
            } else {
                desactivarCuentasDemo(false);
                asegurarCuentasDemo();
            }
            asegurarHistorialDeClientesDemo();
        });
    }

    /**
     * Siembra la agenda de los días del rango que aún no tienen citas demo. Devuelve cuántas citas creó
     * (0 si se vuelve a ejecutar el mismo día: no duplica).
     */
    private int sembrarAgenda(LocalDateTime ahora) {
        GeneradorAgendaDemo.Recursos recursos = new GeneradorAgendaDemo.Recursos(
                catalogo.servicios(), catalogo.cabinas(), catalogo.terapeutas(), asegurarPacientes());
        List<Reserva> existentes = reservaRepository.buscarEntre(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());
        Set<LocalDate> diasSembrados = new HashSet<>();
        List<Reserva> activasExistentes = new ArrayList<>();
        for (Reserva r : existentes) {
            if (esDemo(r.getCliente())) {
                diasSembrados.add(r.getFechaHora().toLocalDate());
            }
            if (r.getEstado() != EstadoReserva.CANCELADA) {
                activasExistentes.add(r);
            }
        }
        GeneradorAgendaDemo generador = new GeneradorAgendaDemo(recursos, ahora);
        List<Reserva> nuevas = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            if (diasSembrados.contains(dia)) {
                continue;
            }
            for (Reserva r : generador.generarDia(dia)) {
                if (activasExistentes.stream().noneMatch(e -> seCruzan(e, r))) {
                    nuevas.add(r);
                }
            }
        }
        reservaRepository.saveAll(nuevas); // el pago se guarda en cascada con su reserva
        return nuevas.size();
    }

    private static boolean esDemo(Cliente c) {
        return c != null && c.getCorreo() != null
                && (c.getCorreo().endsWith(DOMINIO) || c.getCorreo().endsWith(DOMINIO_ANTERIOR));
    }

    /** Misma cabina o misma terapeuta con bloques (sesión + limpieza) que se solapan. */
    private static boolean seCruzan(Reserva a, Reserva b) {
        boolean mismoRecurso = a.getSala().getId().equals(b.getSala().getId())
                || a.getEmpleado().getId().equals(b.getEmpleado().getId());
        return mismoRecurso && a.getFechaHora().isBefore(ReglasReserva.finBloque(b))
                && b.getFechaHora().isBefore(ReglasReserva.finBloque(a));
    }

    /** Crea los pacientes demo que falten (se buscan por correo) y los devuelve en orden fijo. */
    private List<Cliente> asegurarPacientes() {
        Map<String, Cliente> existentes = new HashMap<>();
        for (Cliente c : clienteRepository.findAll()) {
            existentes.put(c.getCorreo(), c);
        }
        List<Cliente> pacientes = new ArrayList<>();
        for (PacientesDemo.Paciente p : PacientesDemo.generar(PACIENTES)) {
            Cliente c = existentes.get(p.correo());
            if (c == null) {
                c = new Cliente();
                c.setNombre(p.nombre());
                c.setCorreo(p.correo());
                c.setTelefono(p.telefono());
                c = clienteRepository.save(c);
            }
            pacientes.add(c);
        }
        return pacientes;
    }

    /**
     * Cuenta de acceso de demostración. No lleva contraseña en el código (el repositorio es público): todas usan la de
     * {@code DEMO_PASSWORD}, que se define en backend/.env. Sin esa variable no se crea ninguna cuenta demo.
     */
    record CuentaDemo(String usuario, String nombre, Rol rol, String terapeuta, String telefono) {
        String correo() {
            return usuario + DOMINIO;
        }
    }

    static final List<CuentaDemo> CUENTAS = List.of(
            new CuentaDemo("admin.demo", "Mateo Salazar", Rol.ADMIN, null, null),
            new CuentaDemo("recepcion.demo", "Camila Morales", Rol.RECEPCIONISTA, null, null),
            new CuentaDemo("ely.demo", "Ely", Rol.ESPECIALISTA, "Ely", null),
            new CuentaDemo("miranda.demo", "Miranda", Rol.ESPECIALISTA, "Miranda", null),
            new CuentaDemo("pamela.demo", "Pamela", Rol.ESPECIALISTA, "Pamela", null),
            new CuentaDemo("maria.demo", "Maria", Rol.ESPECIALISTA, "Maria", null),
            new CuentaDemo("cliente.demo", "Carlos Mendoza", Rol.CLIENTE, null, "900200001"),
            new CuentaDemo("lucia.demo", "Lucía Paredes", Rol.CLIENTE, null, "900200002"),
            new CuentaDemo("andrea.demo", "Andrea Vargas", Rol.CLIENTE, null, "900200003"),
            new CuentaDemo("diego.demo", "Diego Rojas", Rol.CLIENTE, null, "900200004"),
            new CuentaDemo("sofia.demo", "Sofía Medina", Rol.CLIENTE, null, "900200005"));

    /** Crea o actualiza las cuentas de demostración y las vincula a su terapeuta o ficha de cliente. */
    private void asegurarCuentasDemo() {
        for (CuentaDemo c : CUENTAS) {
            Usuario u = usuarioRepository.findByCorreo(c.correo()).orElseGet(Usuario::new);
            u.setCorreo(c.correo());
            u.setNombre(c.nombre());
            u.setRol(c.rol());
            u.setPassword(passwordEncoder.encode(demoPassword));
            u.setActivo(true);
            u.setDebeCambiarClave(false);
            u = usuarioRepository.save(u);
            if (c.terapeuta() != null) {
                final Long usuarioId = u.getId();
                empleadoRepository.findAll().stream().filter(e -> e.getNombre().equals(c.terapeuta())).findFirst().ifPresent(e -> {
                    if (!usuarioId.equals(e.getUsuarioId())) {
                        e.setUsuarioId(usuarioId);
                        empleadoRepository.save(e);
                    }
                });
            }
            if (c.rol() == Rol.CLIENTE) {
                Cliente cliente = clienteRepository.findByCorreo(c.correo()).orElseGet(Cliente::new);
                cliente.setNombre(c.nombre());
                cliente.setCorreo(c.correo());
                cliente.setTelefono(c.telefono());
                cliente.setUsuarioId(u.getId());
                clienteRepository.save(cliente);
            }
        }
        log.info("DemoDataSeeder — {} cuentas de acceso de demostración listas (contraseña: DEMO_PASSWORD).", CUENTAS.size());
    }

    /**
     * Sin DEMO_PASSWORD no hay cuentas demo utilizables: las que existan (de una ejecución anterior) quedan
     * desactivadas. Las del dominio antiguo (@recovr.test) se desactivan siempre: sus contraseñas fijas estuvieron
     * publicadas en el código y en el historial de git.
     */
    private void desactivarCuentasDemo(boolean tambienLasActuales) {
        int n = 0;
        for (Usuario u : usuarioRepository.findAll()) {
            boolean antigua = u.getCorreo().endsWith(DOMINIO_ANTERIOR);
            boolean actual = CUENTAS.stream().anyMatch(c -> c.correo().equalsIgnoreCase(u.getCorreo()));
            if (u.isActivo() && (antigua || (tambienLasActuales && actual))) {
                u.setActivo(false);
                usuarioRepository.save(u);
                n++;
            }
        }
        if (n > 0) {
            log.warn("DemoDataSeeder — {} cuenta(s) demo desactivadas (sin DEMO_PASSWORD o del dominio antiguo).", n);
        }
    }

    /**
     * Los clientes demo con cuenta traen historial propio para que "Mi cuenta" y las fichas de las terapeutas no salgan
     * vacías: tres sesiones completadas, una confirmada próxima y una pendiente, más una ficha y una nota de ejemplo.
     * Solo se crea si el cliente aún no tiene reservas, y cada reserva busca un horario libre (sin cruces).
     */
    private void asegurarHistorialDeClientesDemo() {
        String[] codigos = {"RC-01", "RC-03", "RC-02", "RC-04", "RC-05"};
        Map<String, Servicio> servicios = catalogo.servicios();
        Map<String, Sala> salas = catalogo.cabinas();
        Map<String, Empleado> terapeutas = catalogo.terapeutas();
        int n = 0;
        for (CuentaDemo c : CUENTAS) {
            if (c.rol() != Rol.CLIENTE) {
                continue;
            }
            Cliente cliente = clienteRepository.findByCorreo(c.correo()).orElse(null);
            if (cliente == null || reservaRepository.existsByClienteId(cliente.getId())) {
                continue;
            }
            LocalDate hoy = LocalDate.now(reloj);
            int[] dias = {-35, -21, -9, 3, 6};
            EstadoReserva[] estados = {EstadoReserva.COMPLETADA, EstadoReserva.COMPLETADA, EstadoReserva.COMPLETADA,
                    EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE};
            for (int i = 0; i < dias.length; i++) {
                String codigo = codigos[(n + i) % codigos.length];
                crearReservaSinCruces(cliente, servicios.get(codigo), salas.get(GeneradorAgendaDemo.cabinasDe(codigo).get(0)),
                        terapeutas.get(GeneradorAgendaDemo.terapeutasDe(codigo).get(0)), dentroDelRango(hoy.plusDays(dias[i]), i).atTime(15 + (n % 4), 0), estados[i]);
            }
            n++;
        }
        sembrarFichaDeEjemplo(terapeutas);
    }

    /** Mantiene las fechas de demostración dentro de la ventana sembrada (inicio..fin). */
    private LocalDate dentroDelRango(LocalDate fecha, int posicion) {
        if (fecha.isBefore(inicio)) {
            return inicio.plusDays(posicion + 1L);
        }
        return fecha.isAfter(fin) ? fin.minusDays(posicion) : fecha;
    }

    private boolean hayCruce(Sala sala, Empleado terapeuta, LocalDateTime desde, LocalDateTime hasta) {
        return reservaRepository.buscarEntre(desde.toLocalDate().minusDays(1).atStartOfDay(), desde.toLocalDate().plusDays(2).atStartOfDay())
                .stream().filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .filter(r -> r.getSala().getId().equals(sala.getId()) || r.getEmpleado().getId().equals(terapeuta.getId()))
                .anyMatch(r -> desde.isBefore(ReglasReserva.finBloque(r)) && r.getFechaHora().isBefore(hasta));
    }

    private void crearReservaSinCruces(Cliente cliente, Servicio servicio, Sala sala, Empleado terapeuta,
                                       LocalDateTime deseada, EstadoReserva estado) {
        if (servicio == null || sala == null || terapeuta == null) {
            return;
        }
        LocalDateTime inicio = deseada;
        for (int intento = 0; intento < 200; intento++) {
            LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos() + servicio.getTiempoLimpiezaMinutos());
            if (!hayCruce(sala, terapeuta, inicio, fin)) {
                LocalDateTime ahora = LocalDateTime.now(reloj);
                Reserva r = new Reserva();
                r.setCliente(cliente);
                r.setEmpleado(terapeuta);
                r.setServicio(servicio);
                r.setSala(sala);
                r.setFechaHora(inicio);
                r.setEstado(estado);
                if (estado != EstadoReserva.PENDIENTE) {
                    LocalDateTime creada = inicio.minusDays(2);
                    r.setCreadaEn(creada.isAfter(ahora) ? ahora.minusHours(3) : creada);
                }
                if (estado == EstadoReserva.COMPLETADA) {
                    r.setCheckIn(inicio.minusMinutes(5));
                }
                if (estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.COMPLETADA) {
                    Pago p = new Pago();
                    p.setReserva(r);
                    p.setMonto(servicio.getPrecio());
                    p.setMetodoPago(METODOS_PAGO[Math.floorMod(cliente.getId().intValue() + intento, METODOS_PAGO.length)]);
                    p.setFechaPago(r.getCreadaEn().plusMinutes(5));
                    r.setPago(p);
                }
                reservaRepository.save(r);
                return;
            }
            inicio = inicio.plusMinutes(30);
            if (!ReglasReserva.dentroDeHorario(inicio, inicio.plusMinutes(servicio.getDuracionMinutos()))) {
                // fuera del horario de atención: pasa al día siguiente
                inicio = inicio.toLocalDate().plusDays(1).atTime(12, 0);
            }
        }
    }

    private void sembrarFichaDeEjemplo(Map<String, Empleado> terapeutas) {
        Cliente carlos = clienteRepository.findByCorreo("cliente.demo" + DOMINIO).orElse(null);
        if (carlos == null || fichaRepository.findByClienteId(carlos.getId()).isPresent()) {
            return;
        }
        Empleado atendiente = reservaRepository.buscarPorCliente(carlos.getId()).stream()
                .filter(r -> r.getEstado() == EstadoReserva.COMPLETADA).map(Reserva::getEmpleado).findFirst()
                .orElse(terapeutas.get("Ely"));
        if (atendiente == null) {
            return;
        }
        FichaCliente ficha = new FichaCliente();
        ficha.setCliente(carlos);
        ficha.setAlergias("Esencias de lavanda concentradas");
        ficha.setCondiciones("Sobrecarga lumbar por entrenamiento; sin otras condiciones.");
        ficha.setPresionPreferida("MEDIA");
        ficha.setAceites("Aceite de almendras neutro");
        ficha.setZonasRestringidas("Cervical alta");
        ficha.setNotas("Prefiere música suave y temperatura templada.");
        ficha.setActualizadoPor(atendiente.getNombre());
        ficha.setActualizadoEn(LocalDateTime.now(reloj).minusDays(9));
        fichaRepository.save(ficha);
        NotaSesion nota = new NotaSesion();
        nota.setCliente(carlos);
        nota.setEmpleado(atendiente);
        nota.setFecha(LocalDateTime.now(reloj).minusDays(9));
        nota.setTratamiento("Masaje relajante con foco lumbar");
        nota.setObservaciones("Buena tolerancia a presión media. Contractura leve en el cuadrado lumbar derecho.");
        nota.setRecomendaciones("Estiramientos suaves en casa y agendar mantención en 10 días.");
        notaRepository.save(nota);
    }
}
