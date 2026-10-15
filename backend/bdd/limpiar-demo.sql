-- Elimina TODOS los datos de demostración (los marcados con el dominio @recovr.test) y deja la base lista para uso real.
-- Se conservan: servicios, cabinas, terapeutas oficiales (Ely, Miranda, Pamela y Maria), cuentas que NO sean @recovr.test
-- y todo lo que hayan creado personas reales. Ejecutar con el backend APAGADO:
--   docker exec -i recovr-mysql mysql -uroot -proot recovr_db < backend/bdd/limpiar-demo.sql
-- Después arrancar sin datos demo y con el primer administrador por configuración:
--   ADMIN_EMAIL=dueño@tu-negocio.pe ADMIN_PASSWORD='ClaveInicial123' ./run-real.sh

START TRANSACTION;

CREATE TEMPORARY TABLE demo_clientes AS SELECT id FROM clientes WHERE correo LIKE '%@recovr.test';
CREATE TEMPORARY TABLE demo_reservas AS SELECT id FROM reservas WHERE cliente_id IN (SELECT id FROM demo_clientes);

DELETE FROM notas_sesion  WHERE cliente_id IN (SELECT id FROM demo_clientes) OR reserva_id IN (SELECT id FROM demo_reservas);
DELETE FROM canjes        WHERE cliente_id IN (SELECT id FROM demo_clientes) OR reserva_id IN (SELECT id FROM demo_reservas);
DELETE FROM fichas_cliente WHERE cliente_id IN (SELECT id FROM demo_clientes);
DELETE FROM pagos         WHERE reserva_id IN (SELECT id FROM demo_reservas);
DELETE FROM reservas      WHERE id IN (SELECT id FROM demo_reservas);
DELETE FROM clientes      WHERE id IN (SELECT id FROM demo_clientes);

-- Las terapeutas oficiales quedan, pero sin el acceso de demostración
UPDATE empleados SET usuario_id = NULL WHERE usuario_id IN (SELECT id FROM usuarios WHERE correo LIKE '%@recovr.test');
DELETE FROM usuarios WHERE correo LIKE '%@recovr.test';

-- La auditoría de la demostración tampoco tiene sentido en uso real
DELETE FROM auditoria WHERE usuario LIKE '%@recovr.test';

COMMIT;

SELECT 'clientes' AS tabla, COUNT(*) AS filas FROM clientes
UNION ALL SELECT 'reservas', COUNT(*) FROM reservas
UNION ALL SELECT 'pagos', COUNT(*) FROM pagos
UNION ALL SELECT 'usuarios', COUNT(*) FROM usuarios
UNION ALL SELECT 'terapeutas', COUNT(*) FROM empleados
UNION ALL SELECT 'servicios', COUNT(*) FROM servicios
UNION ALL SELECT 'cabinas', COUNT(*) FROM salas;
