USE MiumgProyectoFinal;
GO

-- 2. Crear tablas 
CREATE TABLE ROL (
    id_rol INT IDENTITY(1,1) PRIMARY KEY,
    tipo_rol VARCHAR(20) NOT NULL
);

CREATE TABLE OPERADOR (
    id_operador INT IDENTITY(1,1) PRIMARY KEY,
    nombre_operador VARCHAR(100) NOT NULL,
    usuario VARCHAR(30) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    id_rol INT NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'Activo',
    FOREIGN KEY (id_rol) REFERENCES ROL(id_rol)
);

CREATE TABLE PAQUETE_TIPO (
    id_paquete_tipo INT IDENTITY(1,1) PRIMARY KEY,
    paquete_tipo VARCHAR(10) NOT NULL UNIQUE CHECK (paquete_tipo IN ('Normal','Fragil'))
);

CREATE TABLE PAQUETE_TAMANO (
    id_paquete_tamano INT IDENTITY(1,1) PRIMARY KEY,
    paquete_tamano VARCHAR(10) NOT NULL UNIQUE CHECK (paquete_tamano IN ('Grande','Mediano','Pequeño'))
);

CREATE TABLE ESPACIO (
    id_espacio VARCHAR(10) PRIMARY KEY,
    tamano VARCHAR(10) NOT NULL CHECK (tamano IN ('Grande','Mediano','Pequeño')),
    tipo_especial VARCHAR(10) NOT NULL CHECK (tipo_especial IN ('Normal','Fragil')),
    capacidad_max INT NOT NULL CHECK (capacidad_max BETWEEN 1 AND 3),
    ocupacion_actual INT NOT NULL DEFAULT 0,
    estado VARCHAR(15) NOT NULL DEFAULT 'Disponible' CHECK (estado IN ('Disponible','Lleno')),
    CHECK (ocupacion_actual BETWEEN 0 AND capacidad_max)
);

CREATE TABLE PAQUETE (
    id VARCHAR(20) PRIMARY KEY,
    id_espacio VARCHAR(10) NULL,
    id_paquete_tipo INT NOT NULL,
    id_paquete_tamano INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Registrado'
        CHECK (estado IN ('Registrado','Asignado','Almacenado','Autorizado','Retirado')),
    fecha_registro DATETIME NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (id_espacio) REFERENCES ESPACIO(id_espacio),
    FOREIGN KEY (id_paquete_tipo) REFERENCES PAQUETE_TIPO(id_paquete_tipo),
    FOREIGN KEY (id_paquete_tamano) REFERENCES PAQUETE_TAMANO(id_paquete_tamano)
);

CREATE TABLE AUTORIZACION_RETIRO (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_paquete VARCHAR(20) NOT NULL,
    fecha_autorizacion DATETIME NOT NULL DEFAULT GETDATE(),
    estado VARCHAR(15) NOT NULL DEFAULT 'Autorizado' CHECK (estado IN ('Autorizado','Utilizado')),
    FOREIGN KEY (id_paquete) REFERENCES PAQUETE(id)
);

CREATE TABLE ALARMA (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_paquete VARCHAR(20) NOT NULL,
    id_espacio VARCHAR(10) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'Activa' CHECK (estado IN ('Activa','Apagada')),
    fecha_activacion DATETIME NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (id_paquete) REFERENCES PAQUETE(id),
    FOREIGN KEY (id_espacio) REFERENCES ESPACIO(id_espacio)
);

CREATE TABLE HISTORIAL_MOV (
    id_movimiento INT IDENTITY(1,1) PRIMARY KEY,
    id_paquete VARCHAR(20) NOT NULL,
    id_espacio VARCHAR(10) NULL,
    id_operador INT NULL,
    accion_realizada VARCHAR(30) NOT NULL,
    fecha_hora DATETIME NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (id_paquete) REFERENCES PAQUETE(id),
    FOREIGN KEY (id_espacio) REFERENCES ESPACIO(id_espacio),
    FOREIGN KEY (id_operador) REFERENCES OPERADOR(id_operador)
);
GO

-- 3. Datos iniciales
INSERT INTO ROL (tipo_rol) VALUES ('Administrador'), ('Operador');

INSERT INTO OPERADOR (nombre_operador, usuario, contrasena, id_rol)
VALUES ('Administrador', 'admin', CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', 'admin123'), 2), 1);

INSERT INTO PAQUETE_TIPO (paquete_tipo) VALUES ('Normal'), ('Fragil');

INSERT INTO PAQUETE_TAMANO (paquete_tamano) VALUES ('Grande'), ('Mediano'), ('Pequeño');

INSERT INTO ESPACIO (id_espacio, tamano, tipo_especial, capacidad_max) VALUES
('E1', 'Grande', 'Normal', 1),
('E2', 'Mediano', 'Normal', 2),
('E3', 'Pequeño', 'Normal', 3),
('E4', 'Grande', 'Fragil', 1),
('E5', 'Mediano', 'Fragil', 2),
('E6', 'Pequeño', 'Fragil', 3);
GO

SELECT @@SERVERNAME;

SELECT *
FROM PAQUETE;