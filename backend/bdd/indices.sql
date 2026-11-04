-- Índices de las consultas de agenda (los crea Hibernate con ddl-auto=update; este script es para bases existentes
-- administradas a mano). Ejecutar una vez:  mysql -u... recovr_db < backend/bdd/indices.sql
CREATE INDEX idx_reservas_fecha          ON reservas (fecha_hora);
CREATE INDEX idx_reservas_empleado_fecha ON reservas (empleado_id, fecha_hora);
CREATE INDEX idx_reservas_sala_fecha     ON reservas (sala_id, fecha_hora);
CREATE INDEX idx_reservas_cliente_fecha  ON reservas (cliente_id, fecha_hora);
CREATE INDEX idx_reservas_estado_fecha   ON reservas (estado, fecha_hora);
CREATE INDEX idx_reservas_estado_creada  ON reservas (estado, creada_en);
CREATE INDEX idx_pagos_fecha             ON pagos (fecha_pago);
