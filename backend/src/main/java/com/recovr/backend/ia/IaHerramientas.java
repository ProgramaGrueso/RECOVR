package com.recovr.backend.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recovr.backend.dto.PerfilTerapeutaDtos.TerapeutaPublica;
import com.recovr.backend.dto.TurnoDisponibleResponse;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.ServicioRepository;
import com.recovr.backend.service.PerfilTerapeutaService;
import com.recovr.backend.service.ReservaService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * Funciones que el modelo puede llamar. Solo leen datos públicos del negocio (carta, terapeutas activas y turnos
 * libres) y pasan por los mismos servicios que el sitio, así que las reglas de la agenda valen igual para el chat.
 * El asistente no crea reservas: en RECOVR reservar exige cuenta de cliente y pago completo, eso se hace en /reserva.
 */
@Component
public class IaHerramientas {

    static final ZoneId LIMA = ZoneId.of("America/Lima");
    static final int MAX_DIAS_ANTICIPACION = 60;

    private final ObjectMapper mapper;
    private final ServicioRepository servicioRepository;
    private final PerfilTerapeutaService perfilTerapeutaService;
    private final ReservaService reservaService;

    public IaHerramientas(ObjectMapper mapper, ServicioRepository servicioRepository,
                          PerfilTerapeutaService perfilTerapeutaService, ReservaService reservaService) {
        this.mapper = mapper;
        this.servicioRepository = servicioRepository;
        this.perfilTerapeutaService = perfilTerapeutaService;
        this.reservaService = reservaService;
    }

    /** Definiciones (JSON Schema) que se envían al modelo. */
    public ArrayNode definiciones() {
        ArrayNode herramientas = mapper.createArrayNode();

        herramientas.add(definicion("listar_tratamientos",
                "Carta vigente de tratamientos con id, nombre, duración en minutos y precio en soles.",
                objeto(), List.of()));

        herramientas.add(definicion("listar_terapeutas",
                "Terapeutas que están atendiendo ahora, con id, nombre y especialidad.",
                objeto(), List.of()));

        herramientas.add(definicion("ver_turnos",
                "Turnos de un día para un tratamiento con una terapeuta: cuáles están libres y por qué no lo están los demás.",
                objeto(
                        propiedad("servicio_id", "integer", "Id del tratamiento (de listar_tratamientos)"),
                        propiedad("terapeuta_id", "integer", "Id de la terapeuta (de listar_terapeutas)"),
                        propiedad("fecha", "string", "Fecha YYYY-MM-DD")),
                List.of("servicio_id", "terapeuta_id", "fecha")));

        return herramientas;
    }

    /** Ejecuta una función y devuelve texto JSON para el modelo. Los errores también son datos. */
    public String ejecutar(String nombre, String argumentosJson) {
        try {
            JsonNode a = mapper.readTree(argumentosJson == null || argumentosJson.isBlank() ? "{}" : argumentosJson);
            return switch (nombre) {
                case "listar_tratamientos" -> listarTratamientos();
                case "listar_terapeutas" -> listarTerapeutas();
                case "ver_turnos" -> verTurnos(a);
                default -> error("Función desconocida.");
            };
        } catch (RuntimeException | java.io.IOException e) {
            return error("No se pudo consultar eso ahora.");
        }
    }

    // ── Implementaciones ──────────────────────────────────────────────────────

    private String listarTratamientos() {
        ArrayNode salida = mapper.createArrayNode();
        servicioRepository.findAll().stream()
                .sorted(Comparator.comparing(Servicio::getId))
                .forEach(s -> {
                    ObjectNode o = salida.addObject();
                    o.put("id", s.getId());
                    o.put("nombre", s.getNombre());
                    o.put("duracion_min", s.getDuracionMinutos() == null ? 60 : s.getDuracionMinutos());
                    o.put("precio_soles", s.getPrecio() == null ? "consultar" : s.getPrecio().toPlainString());
                });
        return salida.toString();
    }

    private String listarTerapeutas() {
        ArrayNode salida = mapper.createArrayNode();
        for (TerapeutaPublica t : perfilTerapeutaService.publicas()) {
            ObjectNode o = salida.addObject();
            o.put("id", t.id());
            o.put("nombre", t.nombre());
            if (t.especialidad() != null) o.put("especialidad", t.especialidad());
        }
        return salida.toString();
    }

    private String verTurnos(JsonNode a) {
        Long servicioId = a.path("servicio_id").canConvertToLong() ? a.path("servicio_id").asLong() : null;
        Long terapeutaId = a.path("terapeuta_id").canConvertToLong() ? a.path("terapeuta_id").asLong() : null;
        LocalDate fecha = fecha(a.path("fecha").asText(""));
        if (servicioId == null || terapeutaId == null || fecha == null) {
            return error("Faltan servicio_id, terapeuta_id o fecha (YYYY-MM-DD).");
        }
        LocalDate hoy = LocalDate.now(LIMA);
        if (fecha.isBefore(hoy)) return error("Esa fecha ya pasó.");
        if (fecha.isAfter(hoy.plusDays(MAX_DIAS_ANTICIPACION))) {
            return error("Solo se reserva con hasta " + MAX_DIAS_ANTICIPACION + " días de anticipación.");
        }

        List<TurnoDisponibleResponse> turnos = reservaService.disponibilidad(terapeutaId, servicioId, fecha, null);
        ArrayNode libres = mapper.createArrayNode();
        ArrayNode ocupados = mapper.createArrayNode();
        for (TurnoDisponibleResponse t : turnos) {
            if (t.disponible()) libres.add(t.hora());
            else ocupados.addObject().put("hora", t.hora()).put("motivo", t.motivo());
        }
        ObjectNode o = mapper.createObjectNode();
        o.put("fecha", fecha.toString());
        o.set("horas_libres", libres);
        o.set("no_disponibles", ocupados);
        return o.toString();
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private String error(String mensaje) {
        return mapper.createObjectNode().put("error", mensaje).toString();
    }

    private LocalDate fecha(String v) {
        try { return v.isBlank() ? null : LocalDate.parse(v); } catch (RuntimeException e) { return null; }
    }

    private ObjectNode definicion(String nombre, String descripcion, ObjectNode parametros, List<String> requeridos) {
        ArrayNode req = parametros.putArray("required");
        requeridos.forEach(req::add);
        ObjectNode fn = mapper.createObjectNode();
        fn.put("name", nombre);
        fn.put("description", descripcion);
        fn.set("parameters", parametros);
        ObjectNode envoltura = mapper.createObjectNode();
        envoltura.put("type", "function");
        envoltura.set("function", fn);
        return envoltura;
    }

    private ObjectNode objeto(ObjectNode... propiedades) {
        ObjectNode esquema = mapper.createObjectNode().put("type", "object");
        ObjectNode props = esquema.putObject("properties");
        for (ObjectNode p : propiedades) {
            props.set(p.get("nombre").asText(), p.get("def"));
        }
        return esquema;
    }

    private ObjectNode propiedad(String nombre, String tipo, String descripcion) {
        ObjectNode p = mapper.createObjectNode().put("nombre", nombre);
        p.putObject("def").put("type", tipo).put("description", descripcion);
        return p;
    }
}
