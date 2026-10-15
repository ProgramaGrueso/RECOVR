package com.recovr.backend.config;

import com.recovr.backend.entity.Empleado;
import com.recovr.backend.repository.ClienteRepository;
import com.recovr.backend.repository.EmpleadoRepository;
import com.recovr.backend.repository.SalaRepository;
import com.recovr.backend.repository.ServicioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Sin datos demo, la base igual arranca con las terapeutas, los servicios y las cabinas oficiales. */
@SpringBootTest(properties = {
        "recovr.seed.catalogo=true",
        "recovr.seed.demo-data=false",
        "spring.datasource.url=jdbc:h2:mem:recovr_catalogotest;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class CatalogoInicialTest {

    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private SalaRepository salaRepository;
    @Autowired private ClienteRepository clienteRepository;

    @Test
    @DisplayName("Sin demo: existen las 4 terapeutas, 6 servicios y 4 cabinas, y ningún cliente ni reserva inventados")
    void catalogoSiempreCreado() {
        assertThat(empleadoRepository.findAll()).extracting(Empleado::getNombre)
                .containsExactlyInAnyOrder("Ely", "Miranda", "Pamela", "Maria");
        assertThat(servicioRepository.count()).isEqualTo(6);
        assertThat(salaRepository.count()).isEqualTo(4);
        assertThat(clienteRepository.count()).isZero();
    }
}
