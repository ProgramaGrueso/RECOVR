-- Verifica la agenda sembrada (o cualquier agenda real) directamente en MySQL.
-- Uso: docker exec -i recovr-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" recovr_db' < backend/bdd/verificar-agenda.sql
-- Cada consulta de solapes debe devolver 0 filas.

-- 1) Misma terapeuta con dos citas activas cuyos bloques (sesión + limpieza) se cruzan
SELECT a.id AS reserva_a, b.id AS reserva_b, a.empleado_id, a.fecha_hora, b.fecha_hora
FROM reservas a
JOIN servicios sa ON sa.id = a.servicio_id
JOIN reservas b ON b.empleado_id = a.empleado_id AND b.id > a.id
JOIN servicios sb ON sb.id = b.servicio_id
WHERE a.estado <> 'CANCELADA' AND b.estado <> 'CANCELADA'
  AND a.fecha_hora < b.fecha_hora + INTERVAL (COALESCE(sb.duracion_minutos, 60) + COALESCE(sb.tiempo_limpieza_minutos, 0)) MINUTE
  AND b.fecha_hora < a.fecha_hora + INTERVAL (COALESCE(sa.duracion_minutos, 60) + COALESCE(sa.tiempo_limpieza_minutos, 0)) MINUTE;

-- 2) Misma cabina
SELECT a.id AS reserva_a, b.id AS reserva_b, a.sala_id, a.fecha_hora, b.fecha_hora
FROM reservas a
JOIN servicios sa ON sa.id = a.servicio_id
JOIN reservas b ON b.sala_id = a.sala_id AND b.id > a.id
JOIN servicios sb ON sb.id = b.servicio_id
WHERE a.estado <> 'CANCELADA' AND b.estado <> 'CANCELADA'
  AND a.fecha_hora < b.fecha_hora + INTERVAL (COALESCE(sb.duracion_minutos, 60) + COALESCE(sb.tiempo_limpieza_minutos, 0)) MINUTE
  AND b.fecha_hora < a.fecha_hora + INTERVAL (COALESCE(sa.duracion_minutos, 60) + COALESCE(sa.tiempo_limpieza_minutos, 0)) MINUTE;

-- 3) Mismo paciente en dos sesiones a la vez (sin contar limpieza)
SELECT a.id AS reserva_a, b.id AS reserva_b, a.cliente_id, a.fecha_hora, b.fecha_hora
FROM reservas a
JOIN servicios sa ON sa.id = a.servicio_id
JOIN reservas b ON b.cliente_id = a.cliente_id AND b.id > a.id
JOIN servicios sb ON sb.id = b.servicio_id
WHERE a.estado <> 'CANCELADA' AND b.estado <> 'CANCELADA'
  AND a.fecha_hora < b.fecha_hora + INTERVAL COALESCE(sb.duracion_minutos, 60) MINUTE
  AND b.fecha_hora < a.fecha_hora + INTERVAL COALESCE(sa.duracion_minutos, 60) MINUTE;

-- 4) Resumen: citas por mes y estado
SELECT DATE_FORMAT(fecha_hora, '%Y-%m') AS mes, estado, COUNT(*) AS citas
FROM reservas GROUP BY mes, estado ORDER BY mes, estado;

-- 5) Promedio de citas por día de la semana (1 = domingo)
SELECT dia_semana, ROUND(AVG(citas), 1) AS promedio
FROM (SELECT DATE(fecha_hora) AS dia, DAYOFWEEK(fecha_hora) AS dia_semana, COUNT(*) AS citas
      FROM reservas GROUP BY dia, dia_semana) t
GROUP BY dia_semana ORDER BY dia_semana;
