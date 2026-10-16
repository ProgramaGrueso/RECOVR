package com.recovr.backend.repository;

import com.recovr.backend.entity.AusenciaEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AusenciaEmpleadoRepository extends JpaRepository<AusenciaEmpleado, Long> {

    List<AusenciaEmpleado> findByEmpleadoIdOrderByDesdeAsc(Long empleadoId);

    /** Ausencias de la terapeuta que se cruzan con [inicio, fin). */
    @Query("SELECT a FROM AusenciaEmpleado a WHERE a.empleado.id = :empleadoId AND a.desde < :fin AND a.hasta > :inicio")
    List<AusenciaEmpleado> buscarCruces(@Param("empleadoId") Long empleadoId,
                                        @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
