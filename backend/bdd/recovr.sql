-- RECOVR: esquema inicial de MySQL
-- Compatible con las entidades JPA actuales y Spring Security/JWT.
-- No incluye credenciales ni datos de prueba.
-- Es seguro ejecutarlo en una base vacia: no elimina tablas existentes.

CREATE DATABASE IF NOT EXISTS recovr_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE recovr_db;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    correo VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL,
    telefono VARCHAR(255),
    usuario_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_clientes_correo UNIQUE (correo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS empleados (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    especialidad VARCHAR(255),
    telefono VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS salas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    capacidad INT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS servicios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    duracion_minutos INT,
    tiempo_limpieza_minutos INT,
    precio DECIMAL(38,2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    empleado_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    sala_id BIGINT NOT NULL,
    fecha_hora DATETIME(6) NOT NULL,
    estado ENUM('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_reservas_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id),
    CONSTRAINT fk_reservas_servicio FOREIGN KEY (servicio_id) REFERENCES servicios (id),
    CONSTRAINT fk_reservas_sala FOREIGN KEY (sala_id) REFERENCES salas (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pagos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reserva_id BIGINT NOT NULL,
    monto DECIMAL(38,2) NOT NULL,
    metodo_pago VARCHAR(255),
    fecha_pago DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_pagos_reserva UNIQUE (reserva_id),
    CONSTRAINT fk_pagos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id)
) ENGINE=InnoDB;

-- Para una base creada antes de agregar limpieza, ejecutar una sola vez:
-- ALTER TABLE servicios ADD COLUMN tiempo_limpieza_minutos INT NULL;

-- Roles validos gestionados por la aplicacion:
-- ADMIN, RECEPCIONISTA, ESPECIALISTA, CLIENTE
