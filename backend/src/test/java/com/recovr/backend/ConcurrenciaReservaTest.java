package com.recovr.backend;

import com.recovr.backend.dto.PagoRequest;
import com.recovr.backend.dto.ReservaRequest;
import com.recovr.backend.entity.Canje;
import com.recovr.backend.entity.Cliente;
import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.entity.Pago;
import com.recovr.backend.entity.Reserva;
import com.recovr.backend.entity.Rol;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.entity.Usuario;
import com.recovr.backend.repository.CanjeRepository;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.PagoRepository;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.repository.UsuarioRepository;
import com.recovr.backend.service.PuntosService;
import com.recovr.backend.service.ReservaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrencia real: dos hilos con transacciones propias (sin {@code @Transactional} en la prueba) compiten por el
 * mismo recurso. Sin el bloqueo por recurso, ambos pasan la consulta de conflictos antes de que el otro guarde y se
 * crean dos reservas; con él, la segunda espera su turno, ve a la primera y recibe el conflicto.
 */
@SpringBootTest
class ConcurrenciaReservaTest {

    private static final int RONDAS = 15;

    @Autowired private ReservaService reservaService;
    @Autowired private PuntosService puntosService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private CanjeRepository canjeRepository;

    private Servicio servicio;
    private final List<Long> usuarios = new ArrayList<>();
    private final List<Long> clientes = new ArrayList<>();
    private final List<Long> empleados = new ArrayList<>();
    private final List<Long> salas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        servicio = new Servicio();
        servicio.setNombre("Servicio concurrencia");
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio = servicioRepository.save(servicio);
    }

    @AfterEach
    void limpiar() {
        canjeRepository.deleteAll(canjeRepository.findAll().stream().filter(c -> clientes.contains(c.getCliente().getId())).toList());
        for (Reserva r : reservaRepository.findAll()) {
            if (clientes.contains(r.getCliente().getId())) {
                reservaRepository.delete(r);
            }
        }
        clienteRepository.deleteAllById(clientes);
        usuarioRepository.deleteAllById(usuarios);
        empleadoRepository.deleteAllById(empleados);
        salaRepository.deleteAllById(salas);
        servicioRepository.deleteById(servicio.getId());
    }

    @Test
    @DisplayName("Misma terapeuta, dos clientes, mismo horario: se crea exactamente una reserva")
    void mismaTerapeuta() throws Exception {
        Empleado maria = empleado("María");
        Sala c1 = sala("C1");
        Sala c2 = sala("C2");
        Cliente a = cliente("a");
        Cliente b = cliente("b");
        for (int i = 0; i < RONDAS; i++) {
            LocalDateTime hora = slot(i);
            List<Resultado> r = enParalelo(
                    () -> crear(a, maria, c1, hora),
                    () -> crear(b, maria, c2, hora));
            assertThat(exitos(r)).as("ronda %d: %s", i, r).isEqualTo(1);
            assertThat(reservaRepository.findAll().stream()
                    .filter(x -> x.getEmpleado().getId().equals(maria.getId()) && x.getFechaHora().equals(hora)
                            && x.getEstado() == EstadoReserva.PENDIENTE)).hasSize(1);
            vaciarReservas();
        }
    }

    @Test
    @DisplayName("Una sola cabina, dos terapeutas, dos clientes: una se queda sin cabina (no se asigna dos veces)")
    void mismaCabinaAutoAsignada() throws Exception {
        Empleado maria = empleado("María");
        Empleado lucia = empleado("Lucía");
        sala("Única");
        Cliente a = cliente("a");
        Cliente b = cliente("b");
        for (int i = 0; i < RONDAS; i++) {
            LocalDateTime hora = slot(i);
            List<Resultado> r = enParalelo(
                    () -> crear(a, maria, null, hora),
                    () -> crear(b, lucia, null, hora));
            assertThat(exitos(r)).as("ronda %d: %s", i, r).isEqualTo(1);
            vaciarReservas();
        }
    }

    @Test
    @DisplayName("El mismo cliente con dos peticiones simultáneas no logra dos sesiones cruzadas ni dos pendientes")
    void mismoClienteDosPeticiones() throws Exception {
        Empleado maria = empleado("María");
        Empleado lucia = empleado("Lucía");
        Sala c1 = sala("C1");
        Sala c2 = sala("C2");
        Cliente a = cliente("a");
        for (int i = 0; i < RONDAS; i++) {
            LocalDateTime hora = slot(i);
            List<Resultado> r = enParalelo(
                    () -> crear(a, maria, c1, hora),
                    () -> crear(a, lucia, c2, hora));
            assertThat(exitos(r)).as("ronda %d: %s", i, r).isEqualTo(1);
            vaciarReservas();
        }
    }

    @Test
    @DisplayName("Dos cobros simultáneos de la misma reserva: se registra un solo pago")
    void doblePagoSimultaneo() throws Exception {
        Empleado maria = empleado("María");
        Sala c1 = sala("C1");
        Cliente a = cliente("a");
        for (int i = 0; i < RONDAS; i++) {
            Reserva reserva = crear(a, maria, c1, slot(i));
            List<Resultado> r = enParalelo(
                    () -> pagar(reserva, a),
                    () -> pagar(reserva, a));
            assertThat(exitos(r)).as("ronda %d: %s", i, r).isEqualTo(1);
            assertThat(pagoRepository.findByReservaId(reserva.getId())).isPresent();
            assertThat(reservaRepository.findById(reserva.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
            vaciarReservas();
        }
    }

    @Test
    @DisplayName("Dos canjes simultáneos con saldo para uno solo: solo uno se concreta")
    void dobleCanjeSimultaneo() throws Exception {
        Empleado maria = empleado("María");
        Sala c1 = sala("C1");
        for (int i = 0; i < RONDAS; i++) {
            Cliente a = cliente("canje" + i);
            // 400 puntos ganados: alcanza para un canje de 300, no para dos
            Reserva completada = new Reserva();
            completada.setCliente(a);
            completada.setEmpleado(maria);
            completada.setSala(c1);
            completada.setServicio(servicio);
            completada.setFechaHora(LocalDateTime.now().minusDays(5));
            completada.setEstado(EstadoReserva.COMPLETADA);
            completada = reservaRepository.save(completada);
            Pago pago = new Pago();
            pago.setReserva(completada);
            pago.setMonto(new BigDecimal("400"));
            pago.setMetodoPago("EFECTIVO");
            pago.setFechaPago(LocalDateTime.now().minusDays(5));
            pagoRepository.save(pago);

            String correo = correoDe(a);
            List<Resultado> r = enParalelo(
                    () -> { puntosService.canjear(correo, "descuento-30"); return null; },
                    () -> { puntosService.canjear(correo, "descuento-30"); return null; });
            assertThat(exitos(r)).as("ronda %d: %s", i, r).isEqualTo(1);
            assertThat(canjeRepository.findByClienteIdOrderByCreadoEnDesc(a.getId())).hasSize(1);
        }
    }

    // ------------------------------------------------------------ utilidades

    private record Resultado(boolean ok, String error) {
        @Override public String toString() { return ok ? "OK" : error; }
    }

    private long exitos(List<Resultado> r) {
        return r.stream().filter(Resultado::ok).count();
    }

    /** Lanza las tareas a la vez (compuerta común) y devuelve qué le pasó a cada una. */
    private List<Resultado> enParalelo(Callable<?>... tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.length);
        CountDownLatch listos = new CountDownLatch(tareas.length);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Resultado>> futuros = new ArrayList<>();
        for (Callable<?> t : tareas) {
            futuros.add(pool.submit(() -> {
                listos.countDown();
                salida.await();
                try {
                    t.call();
                    return new Resultado(true, null);
                } catch (Exception e) {
                    return new Resultado(false, e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }));
        }
        listos.await();
        salida.countDown();
        List<Resultado> resultados = new ArrayList<>();
        for (Future<Resultado> f : futuros) {
            try {
                resultados.add(f.get());
            } catch (ExecutionException e) {
                resultados.add(new Resultado(false, e.getCause().toString()));
            }
        }
        pool.shutdown();
        return resultados;
    }

    private Reserva crear(Cliente c, Empleado e, Sala s, LocalDateTime hora) {
        return reservaService.crearDesdeRequest(
                new ReservaRequest(null, e.getId(), servicio.getId(), s != null ? s.getId() : null, hora, null),
                correoDe(c), false);
    }

    private Reserva pagar(Reserva r, Cliente c) {
        return reservaService.confirmarYPagarParaUsuario(r.getId(),
                new PagoRequest(null, servicio.getPrecio(), "EFECTIVO", null), correoDe(c), false);
    }

    /** Horarios distintos por ronda, dentro del horario de atención. */
    private LocalDateTime slot(int ronda) {
        return LocalDateTime.now().plusDays(3 + ronda).withHour(15).withMinute(0).withSecond(0).withNano(0);
    }

    private void vaciarReservas() {
        for (Reserva r : reservaRepository.findAll()) {
            if (clientes.contains(r.getCliente().getId())) {
                canjeRepository.deleteAll(canjeRepository.findByReservaId(r.getId()));
                reservaRepository.delete(r);
            }
        }
    }

    private String correoDe(Cliente c) {
        return c.getCorreo();
    }

    private Cliente cliente(String id) {
        String correo = id + "-" + System.nanoTime() + "@conc.test";
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setPassword("x");
        u.setRol(Rol.CLIENTE);
        u = usuarioRepository.save(u);
        usuarios.add(u.getId());
        Cliente c = new Cliente();
        c.setNombre("Cliente " + id);
        c.setCorreo(correo);
        c.setUsuarioId(u.getId());
        c = clienteRepository.save(c);
        clientes.add(c.getId());
        return c;
    }

    private Empleado empleado(String nombre) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        e = empleadoRepository.save(e);
        empleados.add(e.getId());
        return e;
    }

    private Sala sala(String nombre) {
        Sala s = new Sala();
        s.setNombre(nombre);
        s.setCapacidad(1);
        s = salaRepository.save(s);
        salas.add(s.getId());
        return s;
    }
}
