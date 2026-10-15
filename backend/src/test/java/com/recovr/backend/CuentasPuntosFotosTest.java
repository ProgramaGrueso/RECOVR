package com.recovr.backend;

import com.recovr.backend.entity.*;
import com.recovr.backend.repository.*;
import com.recovr.backend.security.CustomUserDetailsService;
import com.recovr.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Accesos del personal, programa de puntos con canjes, fotos de perfil y perfil público. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CuentasPuntosFotosTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private CanjeRepository canjeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;

    private Servicio servicio;   // S/ 250
    private Sala sala;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        servicio = new Servicio();
        servicio.setNombre("Servicio Cuentas");
        servicio.setDuracionMinutos(60);
        servicio.setTiempoLimpiezaMinutos(15);
        servicio.setPrecio(new BigDecimal("250.00"));
        servicio = servicioRepository.save(servicio);
        sala = new Sala();
        sala.setNombre("Cabina Cuentas " + UUID.randomUUID());
        sala.setCapacidad(2);
        sala = salaRepository.save(sala);
        admin = usuario(Rol.ADMIN, "Administrador Prueba");
    }

    // ---------------------------------------------------------------- accesos

    @Test
    @DisplayName("El ADMIN crea accesos con clave temporal; la terapeuta se vincula y el acceso exige cambiar la clave")
    void adminCreaAccesos() throws Exception {
        String t = bearer(admin);
        mockMvc.perform(post("/api/usuarios").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Rosa Recepción\",\"correo\":\"Rosa@Recovr.test\",\"rol\":\"RECEPCIONISTA\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuario.correo").value("rosa@recovr.test"))
                .andExpect(jsonPath("$.usuario.rol").value("RECEPCIONISTA")).andExpect(jsonPath("$.usuario.debeCambiarClave").value(true))
                .andExpect(jsonPath("$.claveTemporal").isString());
        // correo repetido
        mockMvc.perform(post("/api/usuarios").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Otra Rosa\",\"correo\":\"rosa@recovr.test\",\"rol\":\"RECEPCIONISTA\"}")).andExpect(status().isBadRequest());
        // los clientes no se crean aquí
        mockMvc.perform(post("/api/usuarios").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Un Cliente\",\"correo\":\"cli@recovr.test\",\"rol\":\"CLIENTE\"}")).andExpect(status().isBadRequest());
        // una ESPECIALISTA necesita su terapeuta, y esa terapeuta no puede tener dos accesos
        mockMvc.perform(post("/api/usuarios").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Sin Vínculo\",\"correo\":\"sv@recovr.test\",\"rol\":\"ESPECIALISTA\"}")).andExpect(status().isBadRequest());
        Empleado e = empleado("Terapeuta Nueva");
        String cuerpo = "{\"nombre\":\"Terapeuta Nueva\",\"correo\":\"tn@recovr.test\",\"rol\":\"ESPECIALISTA\",\"empleadoId\":" + e.getId() + "}";
        mockMvc.perform(post("/api/usuarios").header("Authorization", t).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuario.vinculo").value("Terapeuta Terapeuta Nueva"));
        mockMvc.perform(post("/api/usuarios").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo.replace("tn@", "tn2@"))).andExpect(status().isBadRequest());
        // un ADMIN no se crea con vínculo a terapeuta
        mockMvc.perform(post("/api/usuarios").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Admin Raro\",\"correo\":\"ar@recovr.test\",\"rol\":\"ADMIN\",\"empleadoId\":" + e.getId() + "}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Desactivar un acceso lo corta al instante (también su token); no se puede desactivar a uno mismo ni al último ADMIN")
    void desactivarAccesos() throws Exception {
        String tAdmin = bearer(admin);
        Usuario rec = usuario(Rol.RECEPCIONISTA, "Recepción A");
        String tRec = bearer(rec);
        mockMvc.perform(get("/api/clientes").header("Authorization", tRec)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/usuarios/" + rec.getId() + "/activo?valor=false").header("Authorization", tAdmin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(get("/api/clientes").header("Authorization", tRec)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + rec.getCorreo() + "\",\"password\":\"Clave1234\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/usuarios/" + rec.getId() + "/activo?valor=true").header("Authorization", tAdmin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/clientes").header("Authorization", tRec)).andExpect(status().isOk());

        // sí mismo
        mockMvc.perform(patch("/api/usuarios/" + admin.getId() + "/activo?valor=false").header("Authorization", tAdmin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("propia cuenta")));
        // último admin activo: otro admin intenta desactivar al único restante
        Usuario otro = usuario(Rol.ADMIN, "Admin B");
        usuarioRepository.findAll().stream().filter(u -> u.getRol() == Rol.ADMIN && !u.getId().equals(otro.getId()))
                .forEach(u -> { u.setActivo(false); usuarioRepository.save(u); });
        mockMvc.perform(patch("/api/usuarios/" + otro.getId() + "/activo?valor=false").header("Authorization", bearer(otro)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Restablecer clave genera una temporal, obliga a cambiarla y la política de claves se aplica al cambiarla")
    void restablecerYCambiarClave() throws Exception {
        Usuario rec = usuario(Rol.RECEPCIONISTA, "Recepción B");
        String r = mockMvc.perform(post("/api/usuarios/" + rec.getId() + "/restablecer-clave").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.debeCambiarClave").value(true)).andReturn().getResponse().getContentAsString();
        String temporal = r.replaceAll(".*\"claveTemporal\":\"([^\"]+)\".*", "$1");
        assertThat(temporal).hasSize(12).matches(".*[A-Za-z].*").matches(".*\\d.*");
        // la clave anterior ya no sirve
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + rec.getCorreo() + "\",\"password\":\"Clave1234\"}")).andExpect(status().isUnauthorized());
        String token = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + rec.getCorreo() + "\",\"password\":\"" + temporal + "\"}")).andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        String h = "Bearer " + token;
        for (String mala : new String[]{"corta1", "soloLetrasLargas", "12345678"}) {
            mockMvc.perform(post("/api/auth/cambiar-clave").header("Authorization", h).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"claveActual\":\"" + temporal + "\",\"claveNueva\":\"" + mala + "\"}")).andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/auth/cambiar-clave").header("Authorization", h).contentType(MediaType.APPLICATION_JSON)
                .content("{\"claveActual\":\"" + temporal + "\",\"claveNueva\":\"" + temporal + "\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/cambiar-clave").header("Authorization", h).contentType(MediaType.APPLICATION_JSON)
                .content("{\"claveActual\":\"" + temporal + "\",\"claveNueva\":\"MiClaveNueva9\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", h)).andExpect(jsonPath("$.debeCambiarClave").value(false));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + rec.getCorreo() + "\",\"password\":\"MiClaveNueva9\"}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ya no existe un endpoint público para reclamar la administración")
    void sinRegistroPublicoDeAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/registro-admin").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Intruso\",\"correo\":\"intruso@t.com\",\"password\":\"Clave1234\"}")).andExpect(status().isUnauthorized());
        assertThat(usuarioRepository.existsByCorreo("intruso@t.com")).isFalse();
    }

    // ---------------------------------------------------------------- puntos

    @Test
    @DisplayName("Puntos: se ganan con sesiones completadas pagadas, se canjean por un código y el nivel no baja al canjear")
    void ganarYCanjearPuntos() throws Exception {
        Usuario u = usuario(Rol.CLIENTE, "Cliente Puntos");
        Cliente c = cliente(u);
        Empleado e = empleado("Terapeuta Puntos");
        completada(c, e, 200, 10);   // pagó 200
        completada(c, e, 120, 20);   // pagó 120 (tenía un descuento)
        reserva(c, e, LocalDateTime.now().minusDays(3), EstadoReserva.CANCELADA);   // no suma
        String t = bearer(u);

        mockMvc.perform(get("/api/mi-cuenta/puntos").header("Authorization", t)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ganados").value(320)).andExpect(jsonPath("$.saldo").value(320))
                .andExpect(jsonPath("$.nivel").value("Bronce")).andExpect(jsonPath("$.reglas.length()").value(5))
                .andExpect(jsonPath("$.movimientos.length()").value(2)).andExpect(jsonPath("$.beneficios[0].alcanza").value(true))
                .andExpect(jsonPath("$.beneficios[1].alcanza").value(false));
        // no alcanza para el beneficio de 700
        mockMvc.perform(post("/api/mi-cuenta/canjes").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficioId\":\"descuento-80\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Puntos insuficientes")));
        mockMvc.perform(post("/api/mi-cuenta/canjes").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficioId\":\"no-existe\"}")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/mi-cuenta/canjes").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficioId\":\"descuento-30\"}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(org.hamcrest.Matchers.startsWith("CJ-"))).andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        mockMvc.perform(get("/api/mi-cuenta/puntos").header("Authorization", t))
                .andExpect(jsonPath("$.saldo").value(20)).andExpect(jsonPath("$.ganados").value(320)).andExpect(jsonPath("$.canjeados").value(300))
                .andExpect(jsonPath("$.movimientos.length()").value(3)).andExpect(jsonPath("$.canjes.length()").value(1));
    }

    @Test
    @DisplayName("Al cobrar con un código de canje el servidor recalcula el monto, el código sirve una vez y solo para su dueño")
    void cobrarConCanje() throws Exception {
        Usuario uCli = usuario(Rol.CLIENTE, "Cliente Canje");
        Cliente c = cliente(uCli);
        Cliente otro = cliente(usuario(Rol.CLIENTE, "Otro Cliente"));
        Empleado e = empleado("Terapeuta Canje");
        completada(c, e, 250, 15);
        completada(c, e, 250, 25);   // 500 puntos
        String tCli = bearer(uCli);
        mockMvc.perform(post("/api/mi-cuenta/canjes").header("Authorization", tCli).contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficioId\":\"descuento-30\"}")).andExpect(status().isCreated());
        String codigo = canjeRepository.findByClienteIdOrderByCreadoEnDesc(c.getId()).get(0).getCodigo();

        Reserva r = reserva(c, e, LocalDateTime.now().plusDays(2), EstadoReserva.PENDIENTE);
        Reserva deOtro = reserva(otro, e, LocalDateTime.now().plusDays(4), EstadoReserva.PENDIENTE);
        String tRec = bearer(usuario(Rol.RECEPCIONISTA, "Recepción Canje"));

        // vista previa para recepción: 250 − 30 = 220
        mockMvc.perform(get("/api/canjes/" + codigo.toLowerCase() + "?reservaId=" + r.getId()).header("Authorization", tRec))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valido").value(true)).andExpect(jsonPath("$.montoACobrar").value(220.0));
        mockMvc.perform(get("/api/canjes/" + codigo + "?reservaId=" + deOtro.getId()).header("Authorization", tRec))
                .andExpect(jsonPath("$.valido").value(false)).andExpect(jsonPath("$.motivo").value("El código pertenece a otro cliente"));
        // código de otro cliente no se puede aplicar
        mockMvc.perform(post("/api/db/reservas/" + deOtro.getId() + "/confirmar-y-pagar").header("Authorization", tRec)
                .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":1,\"metodoPago\":\"Efectivo\",\"codigoCanje\":\"" + codigo + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/db/reservas/" + r.getId() + "/confirmar-y-pagar").header("Authorization", tRec)
                .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":1,\"metodoPago\":\"Efectivo\",\"codigoCanje\":\"CJ-NOEXISTE\"}"))
                .andExpect(status().isNotFound());
        // aunque recepción escriba un monto cualquiera, se cobra lo que corresponde
        mockMvc.perform(post("/api/db/reservas/" + r.getId() + "/confirmar-y-pagar").header("Authorization", tRec)
                .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":1,\"metodoPago\":\"Efectivo\",\"codigoCanje\":\"" + codigo + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA")).andExpect(jsonPath("$.precio").value(250.0));
        assertThat(pagoRepository.findAll().stream().filter(p -> p.getReserva().getId().equals(r.getId())).findFirst().orElseThrow().getMonto())
                .isEqualByComparingTo("220.00");
        assertThat(canjeRepository.findByCodigo(codigo).orElseThrow().getEstado()).isEqualTo(Canje.Estado.USADO);
        // ya usado
        Reserva r2 = reserva(c, e, LocalDateTime.now().plusDays(6), EstadoReserva.PENDIENTE);
        mockMvc.perform(post("/api/db/reservas/" + r2.getId() + "/confirmar-y-pagar").header("Authorization", tRec)
                .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":1,\"metodoPago\":\"Efectivo\",\"codigoCanje\":\"" + codigo + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("El código ya fue usado"));
        // sin código, un monto de 0 no es válido
        mockMvc.perform(post("/api/db/reservas/" + r2.getId() + "/confirmar-y-pagar").header("Authorization", tRec)
                .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":0,\"metodoPago\":\"Efectivo\"}")).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- fotos y perfil público

    @Test
    @DisplayName("Foto de perfil: se valida por firma, se re-codifica a JPEG ≤ 1000 px y se publica solo de terapeutas activas")
    void fotoDePerfil() throws Exception {
        Usuario uEsp = usuario(Rol.ESPECIALISTA, "Terapeuta Foto");
        Empleado e = empleado("Terapeuta Foto");
        e.setUsuarioId(uEsp.getId());
        e.setTelefono("999888777");
        e.setBio("Presentación pública");
        empleadoRepository.save(e);
        String tAdmin = bearer(admin);
        String tEsp = bearer(uEsp);

        // sin foto: aparece sin fotoUrl y sin datos de contacto
        String publico = mockMvc.perform(get("/api/publico/terapeutas")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(publico).contains("Terapeuta Foto").contains("Presentación pública").doesNotContain("999888777").doesNotContain("usuarioId");
        mockMvc.perform(get("/api/publico/terapeutas/" + e.getId() + "/foto")).andExpect(status().isNotFound());

        // formatos no permitidos o archivos falsos
        mockMvc.perform(multipart("/api/mi-perfil/foto").file(new MockMultipartFile("archivo", "x.jpg", "image/jpeg", "no soy una imagen".getBytes()))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", tEsp)).andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/mi-perfil/foto").file(new MockMultipartFile("archivo", "x.gif", "image/gif", "GIF89a....".getBytes()))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", tEsp)).andExpect(status().isBadRequest());
        byte[] enorme = new byte[3 * 1024 * 1024 + 10];
        enorme[0] = (byte) 0xFF; enorme[1] = (byte) 0xD8; enorme[2] = (byte) 0xFF;
        mockMvc.perform(multipart("/api/mi-perfil/foto").file(new MockMultipartFile("archivo", "g.jpg", "image/jpeg", enorme))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", tEsp)).andExpect(status().isBadRequest());

        // una terapeuta sube su propia foto grande en PNG
        mockMvc.perform(multipart("/api/mi-perfil/foto").file(new MockMultipartFile("archivo", "yo.png", "image/png", imagen(2000, 1500, "png")))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", tEsp))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tieneFoto").value(true));
        byte[] guardada = mockMvc.perform(get("/api/publico/terapeutas/" + e.getId() + "/foto")).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg")).andReturn().getResponse().getContentAsByteArray();
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(guardada));
        assertThat(leida.getWidth()).isEqualTo(1000);
        assertThat(leida.getHeight()).isEqualTo(750);
        mockMvc.perform(get("/api/publico/terapeutas")).andExpect(jsonPath("$[?(@.nombre=='Terapeuta Foto')].fotoUrl").isNotEmpty());

        // el ADMIN también puede, y una terapeuta dada de baja deja de verse en el sitio
        mockMvc.perform(multipart("/api/empleados/" + e.getId() + "/foto").file(new MockMultipartFile("archivo", "a.jpg", "image/jpeg", imagen(300, 400, "jpg")))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", tAdmin)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/empleados/" + e.getId() + "/activo?valor=false").header("Authorization", tAdmin)).andExpect(status().isOk());
        assertThat(mockMvc.perform(get("/api/publico/terapeutas")).andReturn().getResponse().getContentAsString()).doesNotContain("Terapeuta Foto");
        mockMvc.perform(get("/api/publico/terapeutas/" + e.getId() + "/foto")).andExpect(status().isNotFound());

        // un cliente no puede subir fotos de terapeutas
        mockMvc.perform(multipart("/api/empleados/" + e.getId() + "/foto").file(new MockMultipartFile("archivo", "a.jpg", "image/jpeg", imagen(10, 10, "jpg")))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer(usuario(Rol.CLIENTE, "Cli")))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mi perfil: la terapeuta edita su teléfono y presentación, no su nombre ni su especialidad")
    void miPerfil() throws Exception {
        Usuario uEsp = usuario(Rol.ESPECIALISTA, "Terapeuta Perfil");
        Empleado e = empleado("Terapeuta Perfil");
        e.setUsuarioId(uEsp.getId());
        e.setEspecialidad("Descontracturante");
        empleadoRepository.save(e);
        mockMvc.perform(put("/api/mi-perfil").header("Authorization", bearer(uEsp)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"955111222\",\"bio\":\"Mi nueva presentación\",\"nombre\":\"Hackeado\",\"especialidad\":\"Otra\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefono").value("955111222")).andExpect(jsonPath("$.bio").value("Mi nueva presentación"))
                .andExpect(jsonPath("$.nombre").value("Terapeuta Perfil")).andExpect(jsonPath("$.especialidad").value("Descontracturante"));
    }

    // ---------------------------------------------------------------- utilidades

    private byte[] imagen(int w, int h, String formato) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(java.awt.Color.PINK);
        g.fillRect(0, 0, w, h);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, formato, out);
        return out.toByteArray();
    }

    private Usuario usuario(Rol rol, String nombre) {
        Usuario u = new Usuario();
        u.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@cuentas.test");
        u.setNombre(nombre);
        u.setPassword(passwordEncoder.encode("Clave1234"));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    private String bearer(Usuario u) {
        return "Bearer " + jwtService.generarToken(userDetailsService.loadUserByUsername(u.getCorreo()));
    }

    private Empleado empleado(String nombre) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        return empleadoRepository.save(e);
    }

    private Cliente cliente(Usuario u) {
        Cliente c = new Cliente();
        c.setNombre(u.getNombre());
        c.setCorreo(u.getCorreo());
        c.setUsuarioId(u.getId());
        return clienteRepository.save(c);
    }

    private Reserva reserva(Cliente c, Empleado e, LocalDateTime inicio, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setCliente(c);
        r.setEmpleado(e);
        r.setServicio(servicio);
        r.setSala(sala);
        r.setFechaHora(inicio);
        r.setEstado(estado);
        return reservaRepository.save(r);
    }

    /** Sesión completada hace {@code dias} días, pagada con {@code monto}. */
    private void completada(Cliente c, Empleado e, int monto, int dias) {
        Reserva r = reserva(c, e, LocalDateTime.now().minusDays(dias), EstadoReserva.COMPLETADA);
        Pago p = new Pago();
        p.setReserva(r);
        p.setMonto(new BigDecimal(monto));
        p.setMetodoPago("Yape");
        p.setFechaPago(r.getFechaHora().minusHours(1));
        r.setPago(pagoRepository.save(p));
    }
}
