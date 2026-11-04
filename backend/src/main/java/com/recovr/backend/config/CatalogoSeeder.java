package com.recovr.backend.config;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.entity.Sala;
import com.recovr.backend.entity.Servicio;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo oficial, SIEMPRE presente (con o sin datos demo) y creado de forma idempotente: los 6 servicios RC-xx,
 * las 4 cabinas y las 4 terapeutas (Ely, Miranda, Pamela y Maria), para que la base nunca arranque sin personal.
 * Se apaga con {@code recovr.seed.catalogo=false} (p. ej. en las pruebas que necesitan una base vacía).
 */
@Component
@Order(1)
public class CatalogoSeeder implements CommandLineRunner {

    private record ServicioOficial(String nombre, int duracion, int limpieza, String precio) {}
    private record CabinaOficial(String nombre, int capacidad) {}
    private record TerapeutaOficial(String nombre, String especialidad, String telefono, String bio) {}

    private static final List<ServicioOficial> SERVICIOS = List.of(
            new ServicioOficial("RC-01 Masaje Tántrico Sensitivo", 60, 15, "200"),
            new ServicioOficial("RC-02 Masaje Nuru Body Slide", 75, 15, "280"),
            new ServicioOficial("RC-03 Masaje Relajante & Descontracturante", 60, 15, "180"),
            new ServicioOficial("RC-04 Masaje Tántrico Completo", 75, 15, "250"),
            new ServicioOficial("RC-05 Masaje a Cuatro Manos", 60, 15, "380"),
            new ServicioOficial("RC-06 RECOVR VIP Hidromasaje", 90, 20, "450"));

    private static final List<CabinaOficial> CABINAS = List.of(
            new CabinaOficial("Cabina 01 (Néon Rose)", 2),
            new CabinaOficial("Cabina 02 (Nuru Suite)", 2),
            new CabinaOficial("Cabina 03 (Dúo / 4 Manos)", 3),
            new CabinaOficial("Cabina 04 (VIP Hidromasaje)", 2));

    private static final List<TerapeutaOficial> TERAPEUTAS = List.of(
            new TerapeutaOficial("Ely", "Masaje Tántrico Sensitivo", "900000001",
                    "Especialista en toques lentos, continuos y suaves por todo el cuerpo con aceites tibios neutros."),
            new TerapeutaOficial("Miranda", "Masaje Nuru Body Slide", "900000002",
                    "Especialista en técnicas cuerpo a cuerpo sobre camilla especial con gel nuru tibio hiperdeslizante."),
            new TerapeutaOficial("Pamela", "Descontracturante y Cuatro Manos", "900000003",
                    "Experta en presión media y profunda para liberar sobrecargas musculares y en maniobras sincronizadas a cuatro manos."),
            new TerapeutaOficial("Maria", "Suite Presencial e Hidromasaje", "900000004",
                    "Atención exclusiva en la sesión más completa: masaje tántrico, técnica Nuru cuerpo a cuerpo e hidromasaje."));

    private final boolean activo;
    private final ServicioRepository servicioRepository;
    private final SalaRepository salaRepository;
    private final EmpleadoRepository empleadoRepository;

    public CatalogoSeeder(@Value("${recovr.seed.catalogo:true}") boolean activo, ServicioRepository servicioRepository,
                          SalaRepository salaRepository, EmpleadoRepository empleadoRepository) {
        this.activo = activo;
        this.servicioRepository = servicioRepository;
        this.salaRepository = salaRepository;
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (activo) {
            servicios();
            cabinas();
            terapeutas();
        }
    }

    /** Crea los servicios oficiales que falten y los devuelve indexados por código ("RC-01"…). */
    @Transactional
    public Map<String, Servicio> servicios() {
        Map<String, Servicio> porCodigo = new HashMap<>();
        for (Servicio s : servicioRepository.findAll()) {
            if (s.getNombre().startsWith("RC-")) {
                porCodigo.putIfAbsent(s.getNombre().substring(0, 5), s);
            }
        }
        for (ServicioOficial o : SERVICIOS) {
            porCodigo.computeIfAbsent(o.nombre().substring(0, 5), codigo -> {
                Servicio s = new Servicio();
                s.setNombre(o.nombre());
                s.setDuracionMinutos(o.duracion());
                s.setTiempoLimpiezaMinutos(o.limpieza());
                s.setPrecio(new BigDecimal(o.precio()));
                return servicioRepository.save(s);
            });
        }
        return porCodigo;
    }

    /** Crea las cabinas oficiales que falten y las devuelve indexadas por prefijo ("Cabina 01 "…). */
    @Transactional
    public Map<String, Sala> cabinas() {
        Map<String, Sala> porPrefijo = new HashMap<>();
        for (Sala s : salaRepository.findAll()) {
            if (s.getNombre().matches("Cabina 0\\d .*")) {
                porPrefijo.putIfAbsent(s.getNombre().substring(0, 10), s);
            }
        }
        for (CabinaOficial o : CABINAS) {
            porPrefijo.computeIfAbsent(o.nombre().substring(0, 10), prefijo -> {
                Sala s = new Sala();
                s.setNombre(o.nombre());
                s.setCapacidad(o.capacidad());
                return salaRepository.save(s);
            });
        }
        return porPrefijo;
    }

    /** Crea las terapeutas oficiales que falten (o completa su presentación) y las devuelve indexadas por nombre. */
    @Transactional
    public Map<String, Empleado> terapeutas() {
        Map<String, Empleado> porNombre = new HashMap<>();
        for (Empleado e : empleadoRepository.findAll()) {
            porNombre.putIfAbsent(e.getNombre(), e);
        }
        for (TerapeutaOficial o : TERAPEUTAS) {
            Empleado existente = porNombre.get(o.nombre());
            if (existente == null) {
                Empleado e = new Empleado();
                e.setNombre(o.nombre());
                e.setEspecialidad(o.especialidad());
                e.setTelefono(o.telefono());
                e.setBio(o.bio());
                porNombre.put(o.nombre(), empleadoRepository.save(e));
            } else if (existente.getBio() == null || existente.getBio().isBlank()) {
                existente.setBio(o.bio());
                empleadoRepository.save(existente);
            }
        }
        return porNombre;
    }
}
