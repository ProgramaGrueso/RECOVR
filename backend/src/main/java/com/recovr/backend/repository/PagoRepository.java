package com.recovr.backend.repository;

import com.recovr.backend.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Ingresos cobrados en [desde, hasta), sin contar pagos de reservas canceladas. */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.fechaPago >= :desde "
            + "AND p.fechaPago < :hasta AND p.reserva.estado <> com.recovr.backend.entity.EstadoReserva.CANCELADA")
    java.math.BigDecimal sumarIngresos(@org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
                                       @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);

    boolean existsByReservaId(Long reservaId);
}
