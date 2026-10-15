package com.recovr.backend.security;

import com.recovr.backend.entity.Rol;

import java.util.List;
import java.util.Set;

import static com.recovr.backend.entity.Rol.*;

/**
 * Matriz de permisos documentada. No es decorativa: {@code MatrizPermisosTest} ejecuta cada fila con un token de cada
 * rol y comprueba que los roles permitidos NO reciben 401/403 y que el resto recibe 403. Si alguien cambia
 * {@link SecurityConfig} sin actualizar esta tabla, esa prueba falla.
 */
public final class MatrizPermisos {

    public record Permiso(String accion, String metodo, String ruta, Set<Rol> roles, String nota) {}

    private MatrizPermisos() {}

    public static List<Permiso> todos() {
        return List.of(
                new Permiso("Ver todas las reservas", "GET", "/api/db/reservas", Set.of(ADMIN, RECEPCIONISTA), ""),
                new Permiso("Crear una reserva", "POST", "/api/db/reservas", Set.of(ADMIN, RECEPCIONISTA, CLIENTE), "El cliente reserva siempre a su nombre"),
                new Permiso("Editar una reserva", "PUT", "/api/db/reservas/0", Set.of(ADMIN, RECEPCIONISTA), ""),
                new Permiso("Cancelar una reserva", "PATCH", "/api/db/reservas/0/cancelar", Set.of(ADMIN, RECEPCIONISTA, CLIENTE), "El cliente solo las suyas, con 4 h de anticipación y máximo 2 al mes"),
                new Permiso("Registrar check-in", "POST", "/api/db/reservas/0/check-in", Set.of(ADMIN, RECEPCIONISTA), ""),
                new Permiso("Finalizar una sesión", "POST", "/api/db/reservas/0/finalizar", Set.of(ADMIN, RECEPCIONISTA), ""),
                new Permiso("Cobrar y confirmar", "POST", "/api/db/reservas/0/confirmar-y-pagar", Set.of(ADMIN, RECEPCIONISTA, CLIENTE), "El cliente solo las suyas"),
                new Permiso("Eliminar una reserva cancelada", "DELETE", "/api/db/reservas/0", Set.of(ADMIN), ""),
                new Permiso("Ver mis reservas", "GET", "/api/db/reservas/mias", Set.of(CLIENTE), ""),
                new Permiso("Ver mi agenda de terapeuta", "GET", "/api/db/reservas/agenda", Set.of(ESPECIALISTA), ""),
                new Permiso("Listar clientes", "GET", "/api/clientes", Set.of(ADMIN, RECEPCIONISTA), ""),
                new Permiso("Editar o eliminar clientes", "PUT", "/api/clientes/0", Set.of(ADMIN), ""),
                new Permiso("Ver indicadores y estado en vivo", "GET", "/api/operacion/resumen", Set.of(ADMIN, RECEPCIONISTA), ""),
                new Permiso("Gestionar personal (masajistas)", "POST", "/api/empleados", Set.of(ADMIN), ""),
                new Permiso("Ver el catálogo de terapeutas", "GET", "/api/empleados/catalogo", Set.of(ADMIN, RECEPCIONISTA, ESPECIALISTA, CLIENTE), ""),
                new Permiso("Ver servicios", "GET", "/api/servicios", Set.of(ADMIN, RECEPCIONISTA, ESPECIALISTA, CLIENTE), ""),
                new Permiso("Gestionar servicios", "POST", "/api/servicios", Set.of(ADMIN), ""),
                new Permiso("Gestionar cabinas", "POST", "/api/salas", Set.of(ADMIN), ""),
                new Permiso("Ver pagos", "GET", "/api/pagos", Set.of(ADMIN), ""),
                new Permiso("Ver fichas de clientes", "GET", "/api/fichas/mis-clientes", Set.of(ESPECIALISTA), "Solo de clientes que atiende; ni ADMIN ni RECEPCIONISTA (privacidad)"),
                new Permiso("Mi cuenta (cliente)", "GET", "/api/mi-cuenta/resumen", Set.of(CLIENTE), ""),
                new Permiso("Ver y crear accesos del personal", "GET", "/api/usuarios", Set.of(ADMIN), "El primer ADMIN se crea por configuración; los demás accesos los crea un ADMIN"),
                new Permiso("Desactivar un acceso", "PATCH", "/api/usuarios/0/activo", Set.of(ADMIN), "No puede desactivarse a sí mismo ni al último ADMIN"),
                new Permiso("Restablecer una clave", "POST", "/api/usuarios/0/restablecer-clave", Set.of(ADMIN), "Genera una clave temporal de un solo uso"),
                new Permiso("Cambiar mi contraseña", "POST", "/api/auth/cambiar-clave", Set.of(ADMIN, RECEPCIONISTA, ESPECIALISTA, CLIENTE), "Cada usuario la suya"),
                new Permiso("Subir la foto de una terapeuta", "PUT", "/api/empleados/0/foto", Set.of(ADMIN), "JPG o PNG de hasta 3 MB"),
                new Permiso("Ver y editar mi perfil de terapeuta", "GET", "/api/mi-perfil", Set.of(ESPECIALISTA), "Incluye subir mi propia foto"),
                new Permiso("Ver mis puntos y beneficios", "GET", "/api/mi-cuenta/puntos", Set.of(CLIENTE), ""),
                new Permiso("Canjear puntos", "POST", "/api/mi-cuenta/canjes", Set.of(CLIENTE), "Genera un código de un solo uso"),
                new Permiso("Validar un código de canje", "GET", "/api/canjes/CJ-X?reservaId=0", Set.of(ADMIN, RECEPCIONISTA), ""),
                new Permiso("Estado del sistema", "GET", "/api/sistema/estado", Set.of(ADMIN), ""),
                new Permiso("Auditoría", "GET", "/api/sistema/auditoria", Set.of(ADMIN), ""),
                new Permiso("Matriz de permisos", "GET", "/api/seguridad/matriz", Set.of(ADMIN), "")
        );
    }
}
