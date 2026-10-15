package com.recovr.backend.repository;

import com.recovr.backend.entity.FotoEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FotoEmpleadoRepository extends JpaRepository<FotoEmpleado, Long> {
    /** IDs de terapeutas con foto y su fecha de actualización (sin cargar los bytes). */
    @Query("SELECT f.empleadoId, f.actualizadaEn FROM FotoEmpleado f")
    List<Object[]> fechasDeFotos();
}
