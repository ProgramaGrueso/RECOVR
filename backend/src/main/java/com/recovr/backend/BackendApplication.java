package com.recovr.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.recovr")
public class BackendApplication {

    /** El negocio opera en hora de Lima: los plazos de pago, el check-in y los turnos se miden con ese reloj. */
    public static final String ZONA_NEGOCIO = "America/Lima";

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(ZONA_NEGOCIO));
        SpringApplication.run(BackendApplication.class, args);
    }

}
