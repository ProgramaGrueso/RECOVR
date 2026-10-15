package com.recovr.backend.config;

import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.UsuarioRepository;
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
 * Las cuentas de acceso usan la contraseña recovr.seed.demo-password (DEMO_PASSWORD); si está vacía solo se
 * crean las fichas, sin usuarios.
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
    private final String passwordDemo;
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

    public DemoDataSeeder(@Value("${recovr.seed.catalogo:true}") boolean catalogoInicial,
                          @Value("${recovr.seed.demo-data:false}") boolean activo,
                          @Value("${recovr.seed.demo-password:}") String passwordDemo,
                          @Value("${recovr.seed.inicio:2026-10-01}") LocalDate inicio,
                          @Value("${recovr.seed.fin:2026-12-31}") LocalDate fin,
                          ServicioRepository servicioRepository, SalaRepository salaRepository,
                          EmpleadoRepository empleadoRepository, ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                          PagoRepository pagoRepository, PasswordEncoder passwordEncoder,
                          TransactionTemplate transaccion) {
        this.catalogoInicial = catalogoInicial;
        this.activo = activo;
        this.passwordDemo = passwordDemo;
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
            return;
        }
        int total = transaccion.execute(status -> sembrar());
        log.info("DemoDataSeeder — {} reservas demo creadas ({} → {}).", total, inicio, fin);
    }

    private int sembrar() {
        Map<String, Servicio> servicios = catalogo();
        Map<String, Sala> salas = cabinas();
        Map<String, Empleado> terapeutas = terapeutas();
        List<Cliente> clientes = clientes();
        sembrarCuentasDeStaff(terapeutas);
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
            if (i == 0 && !passwordDemo.isBlank()) {
                c.setUsuarioId(cuenta("cliente.demo" + DOMINIO, Rol.CLIENTE).getId());
                c.setCorreo("cliente.demo" + DOMINIO);
            }
            clientes.add(clienteRepository.save(c));
        }
        return clientes;
    }

    private void sembrarCuentasDeStaff(Map<String, Empleado> terapeutas) {
        if (passwordDemo.isBlank()) {
            log.warn("DemoDataSeeder — DEMO_PASSWORD vacía: no se crean cuentas de acceso demo.");
            return;
        }
        cuenta("admin.demo" + DOMINIO, Rol.ADMIN);
        cuenta("recepcion.demo" + DOMINIO, Rol.RECEPCIONISTA);
        Empleado ely = terapeutas.get("Ely");
        if (ely != null && ely.getUsuarioId() == null) {
            ely.setUsuarioId(cuenta("ely.demo" + DOMINIO, Rol.ESPECIALISTA).getId());
            empleadoRepository.save(ely);
        }
    }

    private Usuario cuenta(String correo, Rol rol) {
        return usuarioRepository.findByCorreo(correo).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setCorreo(correo);
            u.setPassword(passwordEncoder.encode(passwordDemo));
            u.setRol(rol);
            return usuarioRepository.save(u);
        });
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
