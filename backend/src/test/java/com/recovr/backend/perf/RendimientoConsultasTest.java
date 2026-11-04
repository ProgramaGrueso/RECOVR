package com.recovr.backend.perf;

import com.recovr.backend.config.RelojConfig;
import com.recovr.backend.controller.OperacionController;
import com.recovr.backend.dto.ReservaResponse;
import com.recovr.backend.entity.EstadoReserva;
import com.recovr.backend.repository.ReservaRepository;
import com.recovr.backend.service.ReservaService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de rendimiento con los datos del seeder demo (~1.800 citas): cuenta las sentencias SQL que lanza cada
 * operación del panel. Detecta consultas N+1 (una consulta extra por cada fila), que con miles de reservas
 * convierten un listado en cientos de idas y vueltas a la base.
 */
@SpringBootTest(properties = {
        "recovr.seed.catalogo=true",
        "recovr.seed.demo-data=true",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.datasource.url=jdbc:h2:mem:recovr_perftest;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class RendimientoConsultasTest {

    /** Un listado debe resolverse con un número constante de consultas, no proporcional a las filas. */
    static final int MAX_CONSULTAS = 6;

    @Autowired private ReservaService reservaService;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private OperacionController operacionController;
    @Autowired private EntityManagerFactory emf;
    @Autowired private TransactionTemplate transaccion;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    @DisplayName("Listado paginado de reservas (agenda): consultas constantes, sin N+1")
    void listadoSinNMasUno() {
        long consultas = medir("listado 200", () -> reservaService.listarPagina(0, 200).getContent().stream().map(ReservaResponse::de).toList());
        assertThat(consultas).isLessThanOrEqualTo(MAX_CONSULTAS);
    }

    @Test
    @DisplayName("Búsqueda por rango y estado: consultas constantes, sin N+1")
    void busquedaSinNMasUno() {
        LocalDateTime hoy = LocalDateTime.now(RelojConfig.ZONA_NEGOCIO);
        long consultas = medir("buscar 30 días", () -> reservaService.buscarPorRangoYEstado(hoy, hoy.plusDays(30), EstadoReserva.CONFIRMADA)
                .stream().map(ReservaResponse::de).toList());
        assertThat(consultas).isLessThanOrEqualTo(MAX_CONSULTAS);
    }

    @Test
    @DisplayName("Resumen y estado en vivo de cabinas: consultas constantes, sin N+1")
    void resumenSinNMasUno() {
        long resumen = medir("resumen", () -> operacionController.resumen());
        long salas = medir("estado de cabinas", () -> operacionController.salas());
        assertThat(resumen).isLessThanOrEqualTo(MAX_CONSULTAS);
        assertThat(salas).isLessThanOrEqualTo(MAX_CONSULTAS);
    }

    private long medir(String nombre, Runnable operacion) {
        assertThat(reservaRepository.count()).isGreaterThan(1000);
        stats.clear();
        long t0 = System.nanoTime();
        transaccion.executeWithoutResult(s -> operacion.run());
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long consultas = stats.getPrepareStatementCount();
        System.out.printf("RENDIMIENTO %-20s %4d consultas SQL  %5d ms%n", nombre, consultas, ms);
        return consultas;
    }

    @SuppressWarnings("unused")
    private static <T> List<T> nada() { return List.of(); }
}
