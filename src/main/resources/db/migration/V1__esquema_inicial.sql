CREATE SEQUENCE app_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE usuario (
    id NUMBER(19) PRIMARY KEY,
    nombre VARCHAR2(120 CHAR) NOT NULL,
    email VARCHAR2(254 CHAR) UNIQUE,
    telefono VARCHAR2(20 CHAR) UNIQUE,
    password_hash VARCHAR2(100 CHAR) NOT NULL,
    rol VARCHAR2(20 CHAR) NOT NULL CHECK (rol IN ('CLIENTE','INSTRUCTOR','RECEPCION','ADMIN')),
    intentos_fallidos NUMBER(3) DEFAULT 0 NOT NULL,
    bloqueado_hasta TIMESTAMP,
    dni VARCHAR2(8 CHAR) UNIQUE,
    nacimiento DATE,
    direccion VARCHAR2(240 CHAR),
    CHECK (email IS NOT NULL OR telefono IS NOT NULL)
);

CREATE TABLE tipo_clase (
    id NUMBER(19) PRIMARY KEY,
    nombre VARCHAR2(100 CHAR) NOT NULL UNIQUE,
    minutos NUMBER(4) NOT NULL CHECK (minutos BETWEEN 15 AND 480),
    tarifa NUMBER(10,2) NOT NULL CHECK (tarifa > 0),
    activo NUMBER(1) NOT NULL CHECK (activo IN (0,1))
);

CREATE TABLE vehiculo (
    id NUMBER(19) PRIMARY KEY,
    placa VARCHAR2(12 CHAR) NOT NULL UNIQUE,
    descripcion VARCHAR2(140 CHAR) NOT NULL,
    transmision VARCHAR2(12 CHAR) NOT NULL CHECK (transmision IN ('MECANICA','AUTOMATICA')),
    activo NUMBER(1) NOT NULL CHECK (activo IN (0,1))
);

CREATE TABLE horario_instructor (
    id NUMBER(19) PRIMARY KEY,
    instructor_id NUMBER(19) NOT NULL REFERENCES usuario(id),
    inicio TIMESTAMP NOT NULL,
    fin TIMESTAMP NOT NULL,
    CHECK (inicio < fin)
);

CREATE TABLE bloqueo_vehiculo (
    id NUMBER(19) PRIMARY KEY,
    vehiculo_id NUMBER(19) NOT NULL REFERENCES vehiculo(id),
    inicio TIMESTAMP NOT NULL,
    fin TIMESTAMP NOT NULL,
    motivo VARCHAR2(200 CHAR) NOT NULL,
    CHECK (inicio < fin)
);

CREATE TABLE cita (
    id NUMBER(19) PRIMARY KEY,
    alumno_id NUMBER(19) NOT NULL REFERENCES usuario(id),
    instructor_id NUMBER(19) NOT NULL REFERENCES usuario(id),
    tipo_clase_id NUMBER(19) NOT NULL REFERENCES tipo_clase(id),
    vehiculo_id NUMBER(19) REFERENCES vehiculo(id),
    placa_propia VARCHAR2(12 CHAR),
    marca_propia VARCHAR2(70 CHAR),
    modelo_propio VARCHAR2(70 CHAR),
    transmision VARCHAR2(12 CHAR) NOT NULL CHECK (transmision IN ('MECANICA','AUTOMATICA')),
    canal VARCHAR2(15 CHAR) NOT NULL CHECK (canal IN ('WEB','PRESENCIAL','WHATSAPP','FACEBOOK','LLAMADA')),
    inicio TIMESTAMP NOT NULL,
    fin TIMESTAMP NOT NULL,
    vence TIMESTAMP NOT NULL,
    monto NUMBER(10,2) NOT NULL,
    estado VARCHAR2(20 CHAR) NOT NULL CHECK (estado IN
        ('PENDIENTE_PAGO','CONFIRMADA','EN_CURSO','COMPLETADA','CANCELADA','NO_ASISTIO','VENCIDA')),
    motivo VARCHAR2(240 CHAR),
    CHECK (inicio < fin),
    CHECK ((vehiculo_id IS NOT NULL AND placa_propia IS NULL AND marca_propia IS NULL AND modelo_propio IS NULL)
        OR (vehiculo_id IS NULL AND placa_propia IS NOT NULL AND marca_propia IS NOT NULL AND modelo_propio IS NOT NULL))
);

CREATE TABLE pago (
    id NUMBER(19) PRIMARY KEY,
    cita_id NUMBER(19) NOT NULL REFERENCES cita(id),
    medio VARCHAR2(18 CHAR) NOT NULL CHECK (medio IN ('YAPE','PLIN','TRANSFERENCIA')),
    operacion VARCHAR2(60 CHAR) NOT NULL,
    monto NUMBER(10,2) NOT NULL,
    contenido BLOB NOT NULL,
    mime VARCHAR2(40 CHAR) NOT NULL,
    estado VARCHAR2(12 CHAR) NOT NULL CHECK (estado IN ('EN_REVISION','APROBADO','RECHAZADO')),
    creado TIMESTAMP NOT NULL,
    revisado TIMESTAMP,
    revisor_id NUMBER(19) REFERENCES usuario(id),
    motivo VARCHAR2(240 CHAR),
    CONSTRAINT pago_operacion_uk UNIQUE (medio, operacion)
);

CREATE TABLE auditoria (
    id NUMBER(19) PRIMARY KEY,
    actor_id NUMBER(19) REFERENCES usuario(id),
    entidad VARCHAR2(20 CHAR) NOT NULL,
    entidad_id NUMBER(19) NOT NULL,
    accion VARCHAR2(40 CHAR) NOT NULL,
    detalle VARCHAR2(240 CHAR),
    fecha TIMESTAMP NOT NULL
);

CREATE TABLE agenda_control (id NUMBER(1) PRIMARY KEY);
INSERT INTO agenda_control (id) VALUES (1);

CREATE INDEX horario_instructor_ix ON horario_instructor(instructor_id, inicio, fin);
CREATE INDEX bloqueo_vehiculo_ix ON bloqueo_vehiculo(vehiculo_id, inicio, fin);
CREATE INDEX cita_alumno_ix ON cita(alumno_id, inicio);
CREATE INDEX cita_instructor_ix ON cita(instructor_id, inicio, fin);
CREATE INDEX cita_vehiculo_ix ON cita(vehiculo_id, inicio, fin);
CREATE INDEX cita_expiracion_ix ON cita(estado, vence);
CREATE INDEX pago_cita_ix ON pago(cita_id, estado);
CREATE INDEX auditoria_entidad_ix ON auditoria(entidad, entidad_id, fecha);
