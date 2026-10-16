package com.recovr.backend;

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
import com.recovr.backend.security.CustomUserDetailsService;
import com.recovr.backend.security.JwtService;
import com.recovr.backend.service.ReglasReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de negocio de la agenda, el cobro y el crédito, probadas por la API real con JWT:
 * una persona no está en dos sesiones a la vez, no se acaparan cupos sin pagar, solo el pago completo asegura el
 * turno, el horario de atención se respeta en el servidor y una sesión pagada que se cancela deja crédito al cliente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReglasNegocioTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private CanjeRepository canjeRepository;
    @Autowired private com.recovr.backend.repository.FotoEmpleadoRepository fotoRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;

    private String tokenAdmin;
    private String tokenRecepcion;
    private Cliente ana;
    private String tokenAna;
    private Cliente beto;
    private String tokenBeto;
    private Empleado maria;
    private Empleado lucia;
    private Sala cabina1;
    private Sala cabina2;
    private Servicio servicio;      // S/ 120, 60 min + 15 de limpieza
    private Servicio servicioLargo; // S/ 450, 90 min + 20 de limpieza
    private LocalDateTime dia18;    // pasado mañana a las 18:00

    @BeforeEach
    void setUp() {
        dia18 = LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0);
        tokenAdmin = token("admin@rn.com", Rol.ADMIN);
        tokenRecepcion = token("recepcion@rn.com", Rol.RECEPCIONISTA);
        ana = cliente("ana@rn.com");
        tokenAna = token("ana@rn.com", Rol.CLIENTE);
        vincular(ana, "ana@rn.com");
        beto = cliente("beto@rn.com");
        tokenBeto = token("beto@rn.com", Rol.CLIENTE);
        vincular(beto, "beto@rn.com");
        maria = empleado("María");
        lucia = empleado("Lucía");
        cabina1 = sala("Cabina 1");
        cabina2 = sala("Cabina 2");
        servicio = servicio("Relajante", 60, 15, "120.00");
        servicioLargo = servicio("VIP", 90, 20, "450.00");
    }

    // ------------------------------------------------------------ una persona, una sesión a la vez

    @Test
    @DisplayName("Un mismo cliente no puede reservar dos sesiones que se cruzan, aunque sea con otra terapeuta y otra cabina")
    void clienteNoPuedeEstarEnDosSesionesALaVez() throws Exception {
        crear(tokenAna, maria, cabina1, dia18, servicio).andExpect(status().isCreated());
        crear(tokenAna, lucia, cabina2, dia18, servicio)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("dos sesiones a la vez")));
        // aun pagando la primera, la segunda sigue cruzándose con ella
        Reserva primera = reservaRepository.findAll().get(0);
        pagarTotal(primera, tokenAna).andExpect(status().isOk());
        crear(tokenAna, lucia, cabina2, dia18.plusMinutes(30), servicio)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("dos sesiones a la vez")));
    }

    @Test
    @DisplayName("Dos clientes distintos sí pueden reservar a la misma hora con terapeuta y cabina distintas (pareja, amigos)")
    void dosClientesMismaHora() throws Exception {
        crear(tokenAna, maria, cabina1, dia18, servicio).andExpect(status().isCreated());
        crear(tokenBeto, lucia, cabina2, dia18, servicio).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------ usuarios adversariales y reservas pendientes

    @Test
    @DisplayName("Un cliente no puede acaparar cupos: solo una reserva pendiente de pago a la vez")
    void unaSolaPendiente() throws Exception {
        crear(tokenAna, maria, cabina1, dia18, servicio).andExpect(status().isCreated());
        crear(tokenAna, maria, cabina1, dia18.plusDays(1), servicio)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("pendiente de pago")));
        // al pagar la primera, el cupo de "pendientes" se libera y puede reservar otra
        pagarTotal(reservaRepository.findAll().get(0), tokenAna).andExpect(status().isOk());
        crear(tokenAna, maria, cabina1, dia18.plusDays(1), servicio).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Una pendiente sin pagar pierde el cupo a los 15 min de creada: otro cliente puede tomarlo")
    void pendienteVencidaLiberaElCupo() throws Exception {
        Reserva vieja = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        vieja.setCreadaEn(LocalDateTime.now().minusMinutes(ReglasReserva.HOLD_PAGO_MIN + 1));
        reservaRepository.save(vieja);

        crear(tokenBeto, maria, cabina1, dia18, servicio).andExpect(status().isCreated());

        Reserva liberada = reservaRepository.findById(vieja.getId()).orElseThrow();
        assertThat(liberada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(liberada.getObservacion()).isEqualTo(ReglasReserva.EXPIRADA_SIN_PAGO);
    }

    @Test
    @DisplayName("Pagar una reserva cuyo plazo venció se rechaza")
    void noSePuedePagarFueraDePlazo() throws Exception {
        Reserva vieja = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        vieja.setCreadaEn(LocalDateTime.now().minusMinutes(ReglasReserva.HOLD_PAGO_MIN + 1));
        reservaRepository.save(vieja);

        pagarTotal(vieja, tokenAna)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("plazo")));
    }

    @Test
    @DisplayName("Quien deja vencer 3 reservas sin pagar en 24 h queda bloqueado temporalmente (429)")
    void bloqueoPorExpiracionesRepetidas() throws Exception {
        for (int i = 0; i < ReglasReserva.MAX_EXPIRADAS_VENTANA; i++) {
            Reserva r = reserva(ana, maria, cabina1, dia18.plusDays(10 + i), servicio, EstadoReserva.CANCELADA);
            r.setCanceladaEn(LocalDateTime.now().minusHours(2));
            r.setObservacion(ReglasReserva.EXPIRADA_SIN_PAGO);
            reservaRepository.save(r);
        }
        crear(tokenAna, maria, cabina1, dia18, servicio)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("sin pagar")));
        // otro cliente no se ve afectado
        crear(tokenBeto, maria, cabina1, dia18, servicio).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------ pago completo

    @Test
    @DisplayName("Solo el pago total asegura el turno: un monto menor al precio se rechaza y la reserva sigue pendiente")
    void pagoDebeSerCompleto() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        pagar(r, tokenAna, "1.00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("total de la sesión")));
        pagar(r, tokenRecepcion, "119.99").andExpect(status().isBadRequest());
        pagar(r, tokenRecepcion, "120.01").andExpect(status().isBadRequest());
        assertThat(reservaRepository.findById(r.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(pagoRepository.existsByReservaId(r.getId())).isFalse();

        pagar(r, tokenAna, "120.00").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("El estado no se cambia a mano por PUT: confirmar sin pago o completar sin check-in se rechaza")
    void putNoCambiaEstadoAMano() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        actualizar(r, tokenRecepcion, maria, cabina1, dia18, "\"estado\":\"CONFIRMADA\"").andExpect(status().isBadRequest());
        actualizar(r, tokenAdmin, maria, cabina1, dia18, "\"estado\":\"COMPLETADA\"").andExpect(status().isBadRequest());
        assertThat(reservaRepository.findById(r.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    @DisplayName("El pago directo de ADMIN sigue las mismas reglas: total exacto y deja la reserva CONFIRMADA")
    void pagoDirectoDeAdmin() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        mockMvc.perform(auth(post("/api/pagos"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservaId\":" + r.getId() + ",\"monto\":50,\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(auth(post("/api/pagos"), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservaId\":" + r.getId() + ",\"monto\":120.00,\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isCreated());
        assertThat(reservaRepository.findById(r.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("El pago de una sesión confirmada es historial contable: no se borra")
    void pagoConfirmadoNoSeBorra() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        pagarTotal(r, tokenAna).andExpect(status().isOk());
        Long pagoId = pagoRepository.findByReservaId(r.getId()).orElseThrow().getId();
        mockMvc.perform(auth(delete("/api/pagos/" + pagoId), tokenAdmin)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Los puntos salen de lo pagado: una sesión completada sin pago registrado no suma")
    void sinPagoNoHayPuntos() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18.minusDays(30), servicio, EstadoReserva.COMPLETADA);
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/mi-cuenta/resumen"), tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntos").value(0));
        assertThat(r.getPago()).isNull();
    }

    // ------------------------------------------------------------ cancelar una sesión pagada: crédito

    @Test
    @DisplayName("Si el personal cancela una sesión ya pagada, el cliente recibe un crédito por lo pagado")
    void cancelacionDelPersonalDejaCredito() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        pagarTotal(r, tokenAna).andExpect(status().isOk());

        mockMvc.perform(auth(patch("/api/db/reservas/" + r.getId() + "/cancelar"), tokenRecepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        List<Canje> creditos = canjeRepository.findByClienteIdOrderByCreadoEnDesc(ana.getId());
        assertThat(creditos).hasSize(1);
        Canje credito = creditos.get(0);
        assertThat(credito.getBeneficioId()).isEqualTo(ReglasReserva.CREDITO_CANCELACION);
        assertThat(credito.getDescuento()).isEqualByComparingTo("120.00");
        assertThat(credito.getPuntos()).isZero();
        assertThat(credito.getEstado()).isEqualTo(Canje.Estado.DISPONIBLE);
    }

    @Test
    @DisplayName("Cancelar por PUT con estado CANCELADA sigue la misma regla (crédito incluido)")
    void cancelarPorPutTambienAcredita() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        pagarTotal(r, tokenAna).andExpect(status().isOk());
        actualizar(r, tokenAdmin, maria, cabina1, dia18, "\"estado\":\"CANCELADA\"").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertThat(canjeRepository.findByClienteIdOrderByCreadoEnDesc(ana.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Cancelar una reserva sin pago no genera crédito")
    void sinPagoNoHayCredito() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        mockMvc.perform(auth(patch("/api/db/reservas/" + r.getId() + "/cancelar"), tokenAna)).andExpect(status().isOk());
        assertThat(canjeRepository.findByClienteIdOrderByCreadoEnDesc(ana.getId())).isEmpty();
    }

    @Test
    @DisplayName("El crédito se aplica al cobrar otra sesión; si sobra, el saldo queda como crédito nuevo")
    void creditoSeUsaYConservaElSaldo() throws Exception {
        Reserva cara = reserva(ana, maria, cabina1, dia18, servicioLargo, EstadoReserva.PENDIENTE);
        pagarTotal(cara, tokenAna).andExpect(status().isOk());                       // S/ 450
        mockMvc.perform(auth(patch("/api/db/reservas/" + cara.getId() + "/cancelar"), tokenRecepcion)).andExpect(status().isOk());
        Canje credito = canjeRepository.findByClienteIdOrderByCreadoEnDesc(ana.getId()).get(0);
        assertThat(credito.getDescuento()).isEqualByComparingTo("450.00");

        Reserva barata = reserva(ana, lucia, cabina2, dia18.plusDays(1), servicio, EstadoReserva.PENDIENTE); // S/ 120
        mockMvc.perform(auth(post("/api/db/reservas/" + barata.getId() + "/confirmar-y-pagar"), tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":0,\"metodoPago\":\"EFECTIVO\",\"codigoCanje\":\"" + credito.getCodigo() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        assertThat(pagoRepository.findByReservaId(barata.getId()).orElseThrow().getMonto()).isEqualByComparingTo("0");
        assertThat(canjeRepository.findById(credito.getId()).orElseThrow().getEstado()).isEqualTo(Canje.Estado.USADO);
        List<Canje> todos = canjeRepository.findByClienteIdOrderByCreadoEnDesc(ana.getId());
        Canje saldo = todos.stream().filter(c -> c.getEstado() == Canje.Estado.DISPONIBLE).findFirst().orElseThrow();
        assertThat(saldo.getDescuento()).isEqualByComparingTo("330.00");              // 450 − 120
    }

    @Test
    @DisplayName("Al cancelar una sesión pagada con un código de canje, el código vuelve a quedar disponible")
    void cancelarDevuelveElCodigoConsumido() throws Exception {
        Canje c = new Canje();
        c.setCliente(ana);
        c.setBeneficioId("descuento-30");
        c.setTitulo("Descuento de S/ 30");
        c.setPuntos(300);
        c.setDescuento(new BigDecimal("30"));
        c.setCodigo("CJ-TEST01");
        c.setCreadoEn(LocalDateTime.now());
        canjeRepository.save(c);
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        mockMvc.perform(auth(post("/api/db/reservas/" + r.getId() + "/confirmar-y-pagar"), tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":0,\"metodoPago\":\"EFECTIVO\",\"codigoCanje\":\"CJ-TEST01\"}"))
                .andExpect(status().isOk());
        assertThat(canjeRepository.findByCodigo("CJ-TEST01").orElseThrow().getEstado()).isEqualTo(Canje.Estado.USADO);

        mockMvc.perform(auth(patch("/api/db/reservas/" + r.getId() + "/cancelar"), tokenRecepcion)).andExpect(status().isOk());

        Canje devuelto = canjeRepository.findByCodigo("CJ-TEST01").orElseThrow();
        assertThat(devuelto.getEstado()).isEqualTo(Canje.Estado.DISPONIBLE);
        assertThat(devuelto.getReserva()).isNull();
        // y además se acredita lo que sí se pagó en dinero (S/ 90)
        assertThat(canjeRepository.findByClienteIdOrderByCreadoEnDesc(ana.getId()).stream()
                .filter(x -> ReglasReserva.CREDITO_CANCELACION.equals(x.getBeneficioId()))
                .map(Canje::getDescuento)).singleElement().satisfies(d -> assertThat(d).isEqualByComparingTo("90"));
    }

    // ------------------------------------------------------------ horario de atención

    @Test
    @DisplayName("La sesión debe caber en el horario de atención (12:00 a 23:00): el servidor lo exige, no solo la pantalla")
    void horarioDeAtencion() throws Exception {
        LocalDateTime base = dia18.withHour(0);
        crear(tokenAna, maria, cabina1, base.withHour(10), servicio)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("horario de atención")));
        crear(tokenAna, maria, cabina1, base.withHour(3), servicio).andExpect(status().isBadRequest());
        // 21:45 + 90 min termina a las 23:15: fuera
        crear(tokenAna, maria, cabina1, base.withHour(21).withMinute(45), servicioLargo).andExpect(status().isBadRequest());
        // 21:30 + 90 min termina justo a las 23:00: dentro
        crear(tokenAna, maria, cabina1, base.withHour(21).withMinute(30), servicioLargo).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Apertura 12:00 es válida; 11:59 no")
    void bordesDelHorario() throws Exception {
        LocalDateTime base = dia18.withHour(0);
        crear(tokenAna, maria, cabina1, base.withHour(11).withMinute(59), servicio).andExpect(status().isBadRequest());
        crear(tokenAna, maria, cabina1, base.withHour(12), servicio).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------ modificaciones

    @Test
    @DisplayName("Una reserva pagada no puede cambiar de servicio; una con check-in no se puede modificar")
    void modificacionesProtegidas() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        pagarTotal(r, tokenAna).andExpect(status().isOk());
        mockMvc.perform(auth(put("/api/db/reservas/" + r.getId()), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoId\":" + maria.getId() + ",\"servicioId\":" + servicioLargo.getId()
                                + ",\"salaId\":" + cabina1.getId() + ",\"fechaHora\":\"" + fmt(dia18) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("ya está pagada")));

        Reserva conCheckIn = reservaRepository.findById(r.getId()).orElseThrow();
        conCheckIn.setCheckIn(LocalDateTime.now());
        reservaRepository.save(conCheckIn);
        actualizar(r, tokenAdmin, maria, cabina1, dia18.plusHours(1), null).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reprogramar a una fecha pasada se rechaza")
    void reprogramarAlPasado() throws Exception {
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        actualizar(r, tokenAdmin, maria, cabina1, LocalDateTime.now().minusDays(1).withHour(15), null).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ cabinas en mantenimiento y ausencias

    @Test
    @DisplayName("Una cabina en mantenimiento no recibe reservas ni se asigna sola; solo se puede si no tiene reservas activas")
    void cabinaEnMantenimiento() throws Exception {
        mockMvc.perform(auth(patch("/api/salas/" + cabina1.getId() + "/activa?valor=false"), tokenAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
        crear(tokenAna, maria, cabina1, dia18, servicio)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(containsString("mantenimiento")));
        // sin salaId se asigna la otra cabina
        mockMvc.perform(auth(post("/api/db/reservas"), tokenAna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empleadoId\":" + maria.getId() + ",\"servicioId\":" + servicio.getId()
                                + ",\"fechaHora\":\"" + fmt(dia18) + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.salaId").value(cabina2.getId()));
        // cabina2 ya tiene una reserva activa: no puede entrar en mantenimiento
        mockMvc.perform(auth(patch("/api/salas/" + cabina2.getId() + "/activa?valor=false"), tokenAdmin))
                .andExpect(status().isBadRequest());
        // y reactivarla la vuelve a dejar disponible
        mockMvc.perform(auth(patch("/api/salas/" + cabina1.getId() + "/activa?valor=true"), tokenAdmin)).andExpect(status().isOk());
        crear(tokenBeto, lucia, cabina1, dia18, servicio).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Una terapeuta con ausencia registrada no recibe reservas en ese periodo; con reservas activas no se registra")
    void ausenciaDeTerapeuta() throws Exception {
        LocalDateTime desde = dia18.withHour(0);
        mockMvc.perform(auth(post("/api/empleados/" + maria.getId() + "/ausencias"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"" + fmt(desde) + "\",\"hasta\":\"" + fmt(desde.plusDays(3)) + "\",\"motivo\":\"Vacaciones\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.motivo").value("Vacaciones"));

        crear(tokenAna, maria, cabina1, dia18, servicio)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(containsString("ausencia")));
        crear(tokenAna, lucia, cabina1, dia18, servicio).andExpect(status().isCreated());   // otra terapeuta sí
        crear(tokenBeto, maria, cabina1, dia18.plusDays(3).withHour(15), servicio).andExpect(status().isCreated()); // ya volvió

        // con una reserva activa en el periodo no se puede registrar la ausencia
        mockMvc.perform(auth(post("/api/empleados/" + lucia.getId() + "/ausencias"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"" + fmt(desde) + "\",\"hasta\":\"" + fmt(desde.plusDays(1)) + "\",\"motivo\":\"Licencia\"}"))
                .andExpect(status().isBadRequest());
        // rango inválido
        mockMvc.perform(auth(post("/api/empleados/" + lucia.getId() + "/ausencias"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"" + fmt(desde.plusDays(2)) + "\",\"hasta\":\"" + fmt(desde) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ disponibilidad de turnos

    @Test
    @DisplayName("La disponibilidad marca ocupada a la terapeuta, explica el motivo y respeta que el cliente ya tiene otra sesión")
    void disponibilidadDeTurnos() throws Exception {
        String fecha = dia18.toLocalDate().toString();
        crear(tokenBeto, maria, cabina1, dia18, servicio).andExpect(status().isCreated());   // María ocupada 18:00–19:15

        String url = "/api/db/reservas/disponibilidad?empleadoId=" + maria.getId() + "&servicioId=" + servicio.getId() + "&fecha=" + fecha;
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ReglasReserva.TURNOS.size()))
                .andExpect(jsonPath("$[?(@.hora=='18:00')].disponible").value(false))
                .andExpect(jsonPath("$[?(@.hora=='18:00')].motivo").value(org.hamcrest.Matchers.hasItem(containsString("ya tiene una sesión"))))
                .andExpect(jsonPath("$[?(@.hora=='19:30')].disponible").value(true))
                .andExpect(jsonPath("$[?(@.hora=='12:00')].disponible").value(true));

        // Ana ya tiene una reserva con otra terapeuta a las 15:00: con María ese turno no le sirve
        Reserva propia = reserva(ana, lucia, cabina2, dia18.withHour(15), servicio, EstadoReserva.CONFIRMADA);
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), tokenAna))
                .andExpect(jsonPath("$[?(@.hora=='15:00')].disponible").value(false))
                .andExpect(jsonPath("$[?(@.hora=='15:00')].motivo").value(org.hamcrest.Matchers.hasItem(containsString("Ya tienes otra reserva"))));
        // a recepción (que reserva en nombre de otros) no le aplica la restricción del cliente
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), tokenRecepcion))
                .andExpect(jsonPath("$[?(@.hora=='15:00')].disponible").value(true));
        assertThat(propia.getId()).isNotNull();
    }

    @Test
    @DisplayName("La disponibilidad exige sesión iniciada y descarta lo que ya pasó")
    void disponibilidadRequiereSesion() throws Exception {
        String url = "/api/db/reservas/disponibilidad?empleadoId=" + maria.getId() + "&servicioId=" + servicio.getId()
                + "&fecha=" + LocalDateTime.now().minusDays(1).toLocalDate();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), tokenAna))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].disponible").value(false))
                .andExpect(jsonPath("$[0].motivo").value(containsString("ya pasó")));
    }

    // ------------------------------------------------------------ revisión: privacidad, identidad e integridad

    @Test
    @DisplayName("La ficha clínica solo la ve quien atiende al cliente: una reserva cancelada o sin pagar no da acceso")
    void fichaNoSeFiltraPorReservasCanceladas() throws Exception {
        Usuario uEspecialista = usuario("espe@rn.com", Rol.ESPECIALISTA);
        Empleado ely = empleado("Ely");
        ely.setUsuarioId(uEspecialista.getId());
        empleadoRepository.save(ely);
        String tokenEly = jwtService.generarToken(userDetailsService.loadUserByUsername("espe@rn.com"));

        reserva(ana, ely, cabina1, dia18, servicio, EstadoReserva.CANCELADA);
        reserva(beto, ely, cabina2, dia18.plusDays(1), servicio, EstadoReserva.PENDIENTE);
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/fichas/cliente/" + ana.getId()), tokenEly))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/fichas/cliente/" + beto.getId()), tokenEly))
                .andExpect(status().isForbidden());

        reserva(ana, ely, cabina1, dia18.plusDays(2), servicio, EstadoReserva.CONFIRMADA);
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/fichas/cliente/" + ana.getId()), tokenEly))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Si recepción ya creó al cliente, al registrarse en la web se vincula su ficha: no se duplica")
    void registroNoDuplicaAlCliente() throws Exception {
        Cliente mostrador = cliente("carla@rn.com");
        mostrador.setNombre("Carla Mostrador");
        clienteRepository.save(mostrador);
        long antes = clienteRepository.count();

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Carla\",\"correo\":\"carla@rn.com\",\"password\":\"Clave1234\",\"telefono\":\"999\"}"))
                .andExpect(status().is2xxSuccessful());

        assertThat(clienteRepository.count()).isEqualTo(antes);
        Cliente vinculado = clienteRepository.findByCorreo("carla@rn.com").orElseThrow();
        assertThat(vinculado.getId()).isEqualTo(mostrador.getId());
        assertThat(vinculado.getUsuarioId()).isEqualTo(usuarioRepository.findByCorreo("carla@rn.com").orElseThrow().getId());
        // y no se puede registrar dos veces el mismo correo
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\",\"correo\":\"carla@rn.com\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cambiar el correo de un cliente con cuenta actualiza también su usuario de acceso")
    void correoDelClienteSeSincronizaConSuUsuario() throws Exception {
        mockMvc.perform(auth(put("/api/clientes/" + ana.getId()), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Nueva\",\"correo\":\"ana.nueva@rn.com\",\"telefono\":\"900\"}"))
                .andExpect(status().isOk());
        assertThat(usuarioRepository.findByCorreo("ana.nueva@rn.com")).isPresent();
        assertThat(usuarioRepository.findByCorreo("ana@rn.com")).isEmpty();
        // un correo que ya usa otra cuenta se rechaza
        mockMvc.perform(auth(put("/api/clientes/" + ana.getId()), tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"beto@rn.com\",\"telefono\":\"900\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Eliminar a una terapeuta sin historial borra su foto y desactiva su acceso")
    void eliminarEmpleadoNoDejaHuerfanos() throws Exception {
        Usuario u = usuario("sola@rn.com", Rol.ESPECIALISTA);
        Empleado sola = empleado("Sola");
        sola.setUsuarioId(u.getId());
        empleadoRepository.save(sola);
        com.recovr.backend.entity.FotoEmpleado foto = new com.recovr.backend.entity.FotoEmpleado();
        foto.setEmpleadoId(sola.getId());
        foto.setTipo("image/jpeg");
        foto.setDatos(new byte[]{1, 2, 3});
        foto.setActualizadaEn(LocalDateTime.now());
        fotoRepository.save(foto);

        mockMvc.perform(auth(delete("/api/empleados/" + sola.getId()), tokenAdmin)).andExpect(status().isNoContent());

        assertThat(fotoRepository.existsById(sola.getId())).isFalse();
        assertThat(usuarioRepository.findByCorreo("sola@rn.com").orElseThrow().isActivo()).isFalse();
    }

    @Test
    @DisplayName("El listado de reservas va por páginas y trae el total en X-Total-Count")
    void listadoPaginado() throws Exception {
        for (int i = 0; i < 3; i++) {
            reserva(ana, maria, cabina1, dia18.plusDays(i), servicio, EstadoReserva.CONFIRMADA);
        }
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/db/reservas?pagina=0&tamano=2"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/db/reservas?pagina=1&tamano=2"), tokenAdmin))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ------------------------------------------------------------ vigencia y anulación de códigos

    @Test
    @DisplayName("Un código vencido no se puede aplicar al cobrar")
    void codigoVencidoSeRechaza() throws Exception {
        Canje c = canje(ana, "CJ-VENCE1", 300, "30");
        c.setVenceEn(LocalDateTime.now().minusDays(1));
        canjeRepository.save(c);
        Reserva r = reserva(ana, maria, cabina1, dia18, servicio, EstadoReserva.PENDIENTE);
        mockMvc.perform(auth(post("/api/db/reservas/" + r.getId() + "/confirmar-y-pagar"), tokenRecepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":0,\"metodoPago\":\"EFECTIVO\",\"codigoCanje\":\"CJ-VENCE1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("venció")));
    }

    @Test
    @DisplayName("El cliente puede anular un código sin usar y recupera sus puntos; un crédito por cancelación no se anula")
    void anularCodigoDevuelvePuntos() throws Exception {
        Reserva ganada = reserva(ana, maria, cabina1, dia18.minusDays(30), servicio, EstadoReserva.COMPLETADA);
        Pago pago = new Pago();
        pago.setReserva(ganada);
        pago.setMonto(new BigDecimal("400"));
        pago.setMetodoPago("EFECTIVO");
        pago.setFechaPago(LocalDateTime.now().minusDays(30));
        ganada.setPago(pagoRepository.save(pago));
        canje(ana, "CJ-ANULA1", 300, "30");

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder resumen =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/mi-cuenta/resumen");
        mockMvc.perform(auth(resumen, tokenAna)).andExpect(jsonPath("$.puntos").value(100));

        mockMvc.perform(auth(post("/api/mi-cuenta/canjes/CJ-ANULA1/anular"), tokenAna))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ANULADO"));
        mockMvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/mi-cuenta/resumen"), tokenAna))
                .andExpect(jsonPath("$.puntos").value(400));
        // ya anulado no se vuelve a anular, ni se aplica al cobro
        mockMvc.perform(auth(post("/api/mi-cuenta/canjes/CJ-ANULA1/anular"), tokenAna)).andExpect(status().isBadRequest());
        // código de otro cliente
        canje(beto, "CJ-BETO01", 300, "30");
        mockMvc.perform(auth(post("/api/mi-cuenta/canjes/CJ-BETO01/anular"), tokenAna)).andExpect(status().isForbidden());
        // crédito por cancelación (0 puntos): no se anula
        Canje credito = canje(ana, "CJ-CRED01", 0, "120");
        credito.setBeneficioId(ReglasReserva.CREDITO_CANCELACION);
        canjeRepository.save(credito);
        mockMvc.perform(auth(post("/api/mi-cuenta/canjes/CJ-CRED01/anular"), tokenAna)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ no-show reabrible

    @Test
    @DisplayName("Si el cliente llegó tarde, recepción reabre la sesión pagada dada por no asistida mientras siga en curso")
    void reabrirNoShow() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(40);
        Reserva r = reserva(ana, maria, cabina1, inicio, servicio, EstadoReserva.CANCELADA);
        r.setObservacion(ReglasReserva.NO_ASISTIO);
        r.setCanceladaEn(LocalDateTime.now().minusMinutes(5));
        Pago pago = new Pago();
        pago.setReserva(r);
        pago.setMonto(new BigDecimal("120"));
        pago.setMetodoPago("EFECTIVO");
        pago.setFechaPago(inicio.minusDays(1));
        r.setPago(pagoRepository.save(pago));
        reservaRepository.save(r);

        mockMvc.perform(auth(post("/api/db/reservas/" + r.getId() + "/reabrir"), tokenRecepcion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        // una cancelación hecha por una persona no se reabre
        Reserva manual = reserva(beto, lucia, cabina2, inicio, servicio, EstadoReserva.CANCELADA);
        manual.setObservacion(ReglasReserva.CANCELADA_POR_CLIENTE);
        reservaRepository.save(manual);
        mockMvc.perform(auth(post("/api/db/reservas/" + manual.getId() + "/reabrir"), tokenRecepcion))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ helpers

    private ResultActions crear(String token, Empleado e, Sala s, LocalDateTime inicio, Servicio srv) throws Exception {
        return mockMvc.perform(auth(post("/api/db/reservas"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"empleadoId\":" + e.getId() + ",\"servicioId\":" + srv.getId() + ",\"salaId\":" + s.getId()
                        + ",\"fechaHora\":\"" + fmt(inicio) + "\"}"));
    }

    private ResultActions pagarTotal(Reserva r, String token) throws Exception {
        return pagar(r, token, r.getServicio().getPrecio().toPlainString());
    }

    private ResultActions pagar(Reserva r, String token, String monto) throws Exception {
        return mockMvc.perform(auth(post("/api/db/reservas/" + r.getId() + "/confirmar-y-pagar"), token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":" + monto + ",\"metodoPago\":\"EFECTIVO\"}"));
    }

    private ResultActions actualizar(Reserva r, String token, Empleado e, Sala s, LocalDateTime inicio, String extra) throws Exception {
        return mockMvc.perform(auth(put("/api/db/reservas/" + r.getId()), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"empleadoId\":" + e.getId() + ",\"servicioId\":" + r.getServicio().getId() + ",\"salaId\":" + s.getId()
                        + ",\"fechaHora\":\"" + fmt(inicio) + "\"" + (extra != null ? "," + extra : "") + "}"));
    }

    private Reserva reserva(Cliente c, Empleado e, Sala s, LocalDateTime inicio, Servicio srv, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setCliente(c);
        r.setEmpleado(e);
        r.setSala(s);
        r.setServicio(srv);
        r.setFechaHora(inicio);
        r.setEstado(estado);
        r.setCreadaEn(LocalDateTime.now());
        return reservaRepository.save(r);
    }

    private Usuario usuario(String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode("Clave123"));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    private Canje canje(Cliente c, String codigo, int puntos, String descuento) {
        Canje k = new Canje();
        k.setCliente(c);
        k.setBeneficioId("descuento-30");
        k.setTitulo("Descuento");
        k.setPuntos(puntos);
        k.setDescuento(new BigDecimal(descuento));
        k.setCodigo(codigo);
        k.setCreadoEn(LocalDateTime.now());
        k.setVenceEn(LocalDateTime.now().plusDays(30));
        return canjeRepository.save(k);
    }

    private String token(String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode("Clave123"));
        u.setRol(rol);
        usuarioRepository.save(u);
        return jwtService.generarToken(userDetailsService.loadUserByUsername(correo));
    }

    private Cliente cliente(String correo) {
        Cliente c = new Cliente();
        c.setNombre("Cliente " + correo);
        c.setCorreo(correo);
        return clienteRepository.save(c);
    }

    private void vincular(Cliente c, String correo) {
        c.setUsuarioId(usuarioRepository.findByCorreo(correo).orElseThrow().getId());
        clienteRepository.save(c);
    }

    private Empleado empleado(String nombre) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        return empleadoRepository.save(e);
    }

    private Sala sala(String nombre) {
        Sala s = new Sala();
        s.setNombre(nombre);
        s.setCapacidad(1);
        return salaRepository.save(s);
    }

    private Servicio servicio(String nombre, int duracion, int limpieza, String precio) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setDuracionMinutos(duracion);
        s.setTiempoLimpiezaMinutos(limpieza);
        s.setPrecio(new BigDecimal(precio));
        return servicioRepository.save(s);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private static String fmt(LocalDateTime t) {
        return t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
