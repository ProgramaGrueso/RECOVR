package com.recovr.backend.ia;

/** El proveedor de IA no está configurado o no respondió. */
public class IaNoDisponibleException extends RuntimeException {
    public IaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
