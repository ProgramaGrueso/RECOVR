package com.recovr.backend.security;

import org.springframework.security.core.Authentication;

/** Consultas de rol sobre el usuario autenticado, en un solo lugar. */
public final class Roles {

    public static final String ADMIN = "ROLE_ADMIN";
    public static final String RECEPCIONISTA = "ROLE_RECEPCIONISTA";

    private Roles() {}

    /** ADMIN y RECEPCIONISTA gestionan las reservas de cualquier cliente; los demás solo las propias. */
    public static boolean gestionaReservas(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> ADMIN.equals(a.getAuthority()) || RECEPCIONISTA.equals(a.getAuthority()));
    }
}
