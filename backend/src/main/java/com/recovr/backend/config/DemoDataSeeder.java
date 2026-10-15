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
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.service.ReglasReserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Datos iniciales. SIEMPRE (con o sin demo) y de forma idempotente crea el catálogo oficial: los 6 servicios RC-xx,
 * las 4 cabinas y las 4 terapeutas (Ely, Miranda, Pamela y Maria), para que la base nunca arranque sin personal
 * (se apaga con recovr.seed.catalogo=false, p. ej. en las pruebas que necesitan una base vacía).
 *
 * Datos de demostración (solo con recovr.seed.demo-data=true): clientes y reservas
 * repartidas entre {@code recovr.seed.inicio} y {@code recovr.seed.fin} (por defecto octubre a diciembre).
 *
 * Solo corre con recovr.seed.demo-data=true (variable SEED_DEMO_DATA). Todo lo que crea queda marcado con
 * el dominio {@value #DOMINIO} en el correo, y si ya existe un cliente demo se omite (es idempotente).
 * Además crea las cuentas de acceso de demostración (administración, recepción, las 4 terapeutas y 5 clientes),
 * cada una con su propia contraseña, documentadas en backend/CREDENCIALES_DEMO.md. SOLO para desarrollo local.
 *
 * Respeta la regla de negocio del motor de reservas: ni una cabina ni un terapeuta tienen dos reservas
 * activas cuyos bloques [inicio, inicio + duración + limpieza) se crucen. Las sesiones pasadas quedan
 * COMPLETADAS (o CANCELADAS) y las futuras CONFIRMADAS (con pago) o PENDIENTES.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String DOMINIO = "@recovr.test";
    private static final String[] METODOS_PAGO = {"Yape", "Plin", "Efectivo", "Tarjeta"};
    private static final String[] NOMBRES = {"Carlos", "Luis", "Jorge", "Miguel", "José", "Diego", "Renato", "Mateo",
            "Rodrigo", "Álvaro", "Hugo", "Sebastián", "Ricardo", "Fernando", "Andrés", "Gonzalo"};
    private static final String[] APELLIDOS = {"Quispe", "Mendoza", "Flores", "Rojas", "Torres", "Huamán", "Castillo",
            "Vargas", "Chávez", "Ramírez", "Salazar", "Paredes", "Cárdenas", "Medina", "Gutiérrez", "Rivera"};

    private final boolean catalogoInicial;
    private final boolean activo;
    private final LocalDate inicio;
    private final LocalDate fin;

    private final ServicioRepository servicioRepository;
    private final SalaRepository salaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaccion;
    private final FichaClienteRepository fichaRepository;
    private final NotaSesionRepository notaRepository;

    public DemoDataSeeder(@Value("${recovr.seed.catalogo:true}") boolean catalogoInicial,
                          @Value("${recovr.seed.demo-data:false}") boolean activo,
                          @Value("${recovr.seed.inicio:2026-10-01}") LocalDate inicio,
                          @Value("${recovr.seed.fin:2026-12-31}") LocalDate fin,
                          ServicioRepository servicioRepository, SalaRepository salaRepository,
                          EmpleadoRepository empleadoRepository, ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                          PagoRepository pagoRepository, PasswordEncoder passwordEncoder,
                          TransactionTemplate transaccion, FichaClienteRepository fichaRepository,
                          NotaSesionRepository notaRepository) {
        this.catalogoInicial = catalogoInicial;
        this.activo = activo;
        this.inicio = inicio;
        this.fin = fin;
        this.servicioRepository = servicioRepository;
        this.salaRepository = salaRepository;
        this.empleadoRepository = empleadoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.passwordEncoder = passwordEncoder;
        this.transaccion = transaccion;
        this.fichaRepository = fichaRepository;
        this.notaRepository = notaRepository;
    }

    @Override
    public void run(String... args) {
        if (catalogoInicial) {
            transaccion.executeWithoutResult(status -> {
                catalogo();
                cabinas();
                terapeutas();
            });
        }
        if (!activo) {
            return;
        }
        if (clienteRepository.existsByCorreoEndingWith(DOMINIO)) {
            log.info("DemoDataSeeder — ya hay datos demo, se omite.");
        } else {
            int total = transaccion.execute(status -> sembrar());
            log.info("DemoDataSeeder — {} reservas demo creadas ({} → {}).", total, inicio, fin);
        }
        // Las cuentas de acceso se aseguran siempre (idempotente): así quedan con las contraseñas documentadas.
        transaccion.executeWithoutResult(status -> {
            asegurarCuentasDemo();
            asegurarHistorialDeClientesDemo();
        });
    }

    private int sembrar() {
        Map<String, Servicio> servicios = catalogo();
        Map<String, Sala> salas = cabinas();
        Map<String, Empleado> terapeutas = terapeutas();
        List<Cliente> clientes = clientes();
        return sembrarReservas(servicios, salas, terapeutas, clientes);
    }

    /** Crea el catálogo oficial que falte y devuelve los servicios RC-xx indexados por código. */
    private Map<String, Servicio> catalogo() {
        asegurarServicio("RC-01 Masaje Tántrico Sensitivo", 60, 15, "200");
        asegurarServicio("RC-02 Masaje Nuru Body Slide", 75, 15, "280");
        asegurarServicio("RC-03 Masaje Relajante & Descontracturante", 60, 15, "180");
        asegurarServicio("RC-04 Masaje Tántrico Completo", 75, 15, "250");
        asegurarServicio("RC-05 Masaje a Cuatro Manos", 60, 15, "380");
        asegurarServicio("RC-06 RECOVR VIP Hidromasaje", 90, 20, "450");
        Map<String, Servicio> porCodigo = new HashMap<>();
        for (Servicio s : servicioRepository.findAll()) {
            if (s.getNombre().startsWith("RC-")) {
                porCodigo.put(s.getNombre().substring(0, 5), s);
            }
        }
        return porCodigo;
    }

    private void asegurarServicio(String nombre, int duracion, int limpieza, String precio) {
        String codigo = nombre.substring(0, 5);
        if (servicioRepository.findAll().stream().anyMatch(s -> s.getNombre().startsWith(codigo))) {
            return;
        }
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setDuracionMinutos(duracion);
        s.setTiempoLimpiezaMinutos(limpieza);
        s.setPrecio(new BigDecimal(precio));
        servicioRepository.save(s);
    }

    /** Crea las 4 cabinas oficiales que falten y las devuelve indexadas por "Cabina 0X ". */
    private Map<String, Sala> cabinas() {
        asegurarSala("Cabina 01 (Néon Rose)", 2);
        asegurarSala("Cabina 02 (Nuru Suite)", 2);
        asegurarSala("Cabina 03 (Dúo / 4 Manos)", 3);
        asegurarSala("Cabina 04 (VIP Hidromasaje)", 2);
        Map<String, Sala> porNumero = new HashMap<>();
        for (Sala s : salaRepository.findAll()) {
            if (s.getNombre().matches("Cabina 0\\d .*")) {
                porNumero.put(s.getNombre().substring(0, 10), s);
            }
        }
        return porNumero;
    }

    private void asegurarSala(String nombre, int capacidad) {
        String prefijo = nombre.substring(0, 10);
        if (salaRepository.findAll().stream().anyMatch(s -> s.getNombre().startsWith(prefijo))) {
            return;
        }
        Sala s = new Sala();
        s.setNombre(nombre);
        s.setCapacidad(capacidad);
        salaRepository.save(s);
    }

    private Map<String, Empleado> terapeutas() {
        asegurarEmpleado("Ely", "Masaje Tántrico Sensitivo", "900000001");
        asegurarEmpleado("Miranda", "Masaje Nuru Body Slide", "900000002");
        asegurarEmpleado("Pamela", "Descontracturante y Cuatro Manos", "900000003");
        asegurarEmpleado("Maria", "Suite Presencial e Hidromasaje", "900000004");
        Map<String, Empleado> porNombre = new HashMap<>();
        for (Empleado e : empleadoRepository.findAll()) {
            porNombre.put(e.getNombre(), e);
        }
        return porNombre;
    }

    private void asegurarEmpleado(String nombre, String especialidad, String telefono) {
        if (empleadoRepository.findAll().stream().anyMatch(e -> e.getNombre().equals(nombre))) {
            return;
        }
        Empleado e = new Empleado();
        e.setNombre(nombre);
        e.setEspecialidad(especialidad);
        e.setTelefono(telefono);
        empleadoRepository.save(e);
    }

    private List<Cliente> clientes() {
        Random rnd = new Random(2026);
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            Cliente c = new Cliente();
            c.setNombre(NOMBRES[rnd.nextInt(NOMBRES.length)] + " " + APELLIDOS[rnd.nextInt(APELLIDOS.length)]);
            c.setCorreo("cliente" + i + DOMINIO);
            c.setTelefono("9" + (10000000 + rnd.nextInt(89999999)));
            if (i == 0) {
                c.setCorreo("cliente.demo" + DOMINIO); // su cuenta de acceso se crea en asegurarCuentasDemo()
            }
            clientes.add(clienteRepository.save(c));
        }
        return clientes;
    }

    /** Cuenta de acceso de demostración. La contraseña es propia de cada cuenta (ver CREDENCIALES_DEMO.md). */
    record CuentaDemo(String usuario, String nombre, Rol rol, String password, String terapeuta, String telefono) {
        String correo() {
            return usuario + DOMINIO;
        }
    }

    static final List<CuentaDemo> CUENTAS = List.of(
            new CuentaDemo("admin.demo", "Mateo Salazar", Rol.ADMIN, "Admin#Recovr2026", null, null),
            new CuentaDemo("recepcion.demo", "Camila Morales", Rol.RECEPCIONISTA, "Recep#Recovr2026", null, null),
            new CuentaDemo("ely.demo", "Ely", Rol.ESPECIALISTA, "Ely#Recovr2026", "Ely", null),
            new CuentaDemo("miranda.demo", "Miranda", Rol.ESPECIALISTA, "Miranda#Recovr2026", "Miranda", null),
            new CuentaDemo("pamela.demo", "Pamela", Rol.ESPECIALISTA, "Pamela#Recovr2026", "Pamela", null),
            new CuentaDemo("maria.demo", "Maria", Rol.ESPECIALISTA, "Maria#Recovr2026", "Maria", null),
            new CuentaDemo("cliente.demo", "Carlos Mendoza", Rol.CLIENTE, "Carlos#Recovr2026", null, "987654321"),
            new CuentaDemo("lucia.demo", "Lucía Paredes", Rol.CLIENTE, "Lucia#Recovr2026", null, "987000002"),
            new CuentaDemo("andrea.demo", "Andrea Vargas", Rol.CLIENTE, "Andrea#Recovr2026", null, "987000003"),
            new CuentaDemo("diego.demo", "Diego Rojas", Rol.CLIENTE, "Diego#Recovr2026", null, "987000004"),
            new CuentaDemo("sofia.demo", "Sofía Medina", Rol.CLIENTE, "Sofia#Recovr2026", null, "987000005"));

    /** Crea o actualiza las cuentas de demostración y las vincula a su terapeuta o ficha de cliente. */
    private void asegurarCuentasDemo() {
        for (CuentaDemo c : CUENTAS) {
            Usuario u = usuarioRepository.findByCorreo(c.correo()).orElseGet(Usuario::new);
            u.setCorreo(c.correo());
            u.setNombre(c.nombre());
            u.setRol(c.rol());
            u.setPassword(passwordEncoder.encode(c.password()));
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
        log.info("DemoDataSeeder — {} cuentas de acceso de demostración listas (ver backend/CREDENCIALES_DEMO.md).", CUENTAS.size());
    }

    /**
     * Los clientes demo con cuenta traen historial propio para que "Mi cuenta" y las fichas de las terapeutas no salgan
     * vacías: tres sesiones completadas, una confirmada próxima y una pendiente, más una ficha y una nota de ejemplo.
     * Solo se crea si el cliente aún no tiene reservas, y cada reserva busca un horario libre (sin cruces).
     */
    private void asegurarHistorialDeClientesDemo() {
        String[] codigos = {"RC-01", "RC-03", "RC-02", "RC-04", "RC-05"};
        Map<String, Servicio> servicios = catalogo();
        Map<String, Sala> salas = cabinas();
        Map<String, Empleado> terapeutas = terapeutas();
        int n = 0;
        for (CuentaDemo c : CUENTAS) {
            if (c.rol() != Rol.CLIENTE) {
                continue;
            }
            Cliente cliente = clienteRepository.findByCorreo(c.correo()).orElse(null);
            if (cliente == null || reservaRepository.existsByClienteId(cliente.getId())) {
                continue;
            }
            LocalDate hoy = LocalDate.now();
            int[] dias = {-35, -21, -9, 3, 6};
            EstadoReserva[] estados = {EstadoReserva.COMPLETADA, EstadoReserva.COMPLETADA, EstadoReserva.COMPLETADA,
                    EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE};
            for (int i = 0; i < dias.length; i++) {
                String codigo = codigos[(n + i) % codigos.length];
                crearReservaSinCruces(cliente, servicios.get(codigo), salas.get(cabinaDe(codigo)),
                        terapeutas.get(terapeutaDe(codigo)), dentroDelRango(hoy.plusDays(dias[i]), i).atTime(15 + (n % 4), 0), estados[i]);
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
        for (int intento = 0; intento < 40; intento++) {
            LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos() + servicio.getTiempoLimpiezaMinutos());
            if (!hayCruce(sala, terapeuta, inicio, fin)) {
                Reserva r = new Reserva();
                r.setCliente(cliente);
                r.setEmpleado(terapeuta);
                r.setServicio(servicio);
                r.setSala(sala);
                r.setFechaHora(inicio);
                r.setEstado(estado);
                reservaRepository.save(r);
                if (estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.COMPLETADA) {
                    Pago p = new Pago();
                    p.setReserva(r);
                    p.setMonto(servicio.getPrecio());
                    p.setMetodoPago(METODOS_PAGO[(int) (r.getId() % METODOS_PAGO.length)]);
                    p.setFechaPago(inicio.isBefore(LocalDateTime.now()) ? inicio.minusHours(1) : LocalDateTime.now().minusHours(2));
                    pagoRepository.save(p);
                }
                return;
            }
            inicio = inicio.plusMinutes(30);
            if (inicio.getHour() >= 21) { // fuera del horario de atención: pasa al día siguiente
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
        ficha.setActualizadoEn(LocalDateTime.now().minusDays(9));
        fichaRepository.save(ficha);
        NotaSesion nota = new NotaSesion();
        nota.setCliente(carlos);
        nota.setEmpleado(atendiente);
        nota.setFecha(LocalDateTime.now().minusDays(9));
        nota.setTratamiento("Masaje relajante con foco lumbar");
        nota.setObservaciones("Buena tolerancia a presión media. Contractura leve en el cuadrado lumbar derecho.");
        nota.setRecomendaciones("Estiramientos suaves en casa y agendar mantención en 10 días.");
        notaRepository.save(nota);
    }

    /** Qué terapeuta y qué cabina atienden cada servicio (según la carta oficial de RECOVR). */
    private static String terapeutaDe(String codigo) {
        return switch (codigo) {
            case "RC-01", "RC-04" -> "Ely";
            case "RC-02" -> "Miranda";
            case "RC-03", "RC-05" -> "Pamela";
            default -> "Maria";
        };
    }

    private static String cabinaDe(String codigo) {
        return switch (codigo) {
            case "RC-02" -> "Cabina 02 ";
            case "RC-05" -> "Cabina 03 ";
            case "RC-06" -> "Cabina 04 ";
            default -> "Cabina 01 ";
        };
    }

    private int sembrarReservas(Map<String, Servicio> servicios, Map<String, Sala> salas,
                                Map<String, Empleado> terapeutas, List<Cliente> clientes) {
        Random rnd = new Random(7);
        LocalDateTime ahora = LocalDateTime.now();
        List<String> codigos = new ArrayList<>(servicios.keySet());
        codigos.sort(String::compareTo);
        Map<Long, List<LocalDateTime[]>> ocupadoPorSala = new HashMap<>();
        Map<Long, List<LocalDateTime[]>> ocupadoPorTerapeuta = new HashMap<>();
        List<Reserva> reservas = new ArrayList<>();
        List<Pago> pagos = new ArrayList<>();

        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            boolean finDeSemana = dia.getDayOfWeek().getValue() >= 5;
            int intentos = (finDeSemana ? 9 : 6) + rnd.nextInt(4);
            for (int n = 0; n < intentos; n++) {
                String codigo = codigos.get(rnd.nextInt(codigos.size()));
                Servicio servicio = servicios.get(codigo);
                Sala sala = salas.get(cabinaDe(codigo));
                Empleado terapeuta = terapeutas.get(terapeutaDe(codigo));
                if (sala == null || terapeuta == null) {
                    continue;
                }
                LocalDateTime desde = dia.atTime(LocalTime.of(12 + rnd.nextInt(10), rnd.nextBoolean() ? 0 : 30));
                LocalDateTime hasta = desde.plusMinutes(servicio.getDuracionMinutos() + servicio.getTiempoLimpiezaMinutos());
                if (hayCruce(ocupadoPorSala, sala.getId(), desde, hasta)
                        || hayCruce(ocupadoPorTerapeuta, terapeuta.getId(), desde, hasta)) {
                    continue;
                }
                EstadoReserva estado = elegirEstado(desde, ahora, rnd);
                if (estado == EstadoReserva.PENDIENTE || estado == EstadoReserva.CONFIRMADA
                        || estado == EstadoReserva.COMPLETADA) {
                    // Solo las reservas que ocupan agenda bloquean horarios; las canceladas liberan el turno.
                    ocupadoPorSala.computeIfAbsent(sala.getId(), k -> new ArrayList<>()).add(new LocalDateTime[]{desde, hasta});
                    ocupadoPorTerapeuta.computeIfAbsent(terapeuta.getId(), k -> new ArrayList<>()).add(new LocalDateTime[]{desde, hasta});
                }
                Reserva r = new Reserva();
                r.setCliente(clientes.get(rnd.nextInt(clientes.size())));
                r.setEmpleado(terapeuta);
                r.setServicio(servicio);
                r.setSala(sala);
                r.setFechaHora(desde);
                r.setEstado(estado);
                reservas.add(r);
            }
        }
        reservaRepository.saveAll(reservas);
        for (Reserva r : reservas) {
            if (r.getEstado() == EstadoReserva.CONFIRMADA || r.getEstado() == EstadoReserva.COMPLETADA) {
                Pago p = new Pago();
                p.setReserva(r);
                p.setMonto(r.getServicio().getPrecio());
                p.setMetodoPago(METODOS_PAGO[rnd.nextInt(METODOS_PAGO.length)]);
                p.setFechaPago(r.getFechaHora().isBefore(ahora)
                        ? r.getFechaHora().minusHours(1)
                        : ahora.minusMinutes(rnd.nextInt(60 * 24 * 3)));
                pagos.add(p);
            }
        }
        pagoRepository.saveAll(pagos);
        return reservas.size();
    }

    private static EstadoReserva elegirEstado(LocalDateTime inicioSesion, LocalDateTime ahora, Random rnd) {
        double x = rnd.nextDouble();
        if (inicioSesion.isBefore(ahora)) {
            return x < 0.88 ? EstadoReserva.COMPLETADA : EstadoReserva.CANCELADA;
        }
        if (x < 0.07) {
            return EstadoReserva.CANCELADA;
        }
        boolean proxima = inicioSesion.isBefore(ahora.plusDays(7));
        return x < (proxima ? 0.85 : 0.55) ? EstadoReserva.CONFIRMADA : EstadoReserva.PENDIENTE;
    }

    private static boolean hayCruce(Map<Long, List<LocalDateTime[]>> ocupado, Long id, LocalDateTime desde, LocalDateTime hasta) {
        return ocupado.getOrDefault(id, List.of()).stream()
                .anyMatch(b -> desde.isBefore(b[1]) && b[0].isBefore(hasta));
    }
}
