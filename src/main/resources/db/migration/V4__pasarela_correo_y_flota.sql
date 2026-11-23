ALTER TABLE pago DROP CONSTRAINT pago_medio_ck;
ALTER TABLE pago ADD CONSTRAINT pago_medio_ck
    CHECK (medio IN ('YAPE','PLIN','TRANSFERENCIA','EFECTIVO','MERCADO_PAGO'));

CREATE TABLE pago_checkout (
    cita_id NUMBER(19) PRIMARY KEY REFERENCES cita(id),
    referencia VARCHAR2(36 CHAR) NOT NULL UNIQUE,
    orden_externa VARCHAR2(100 CHAR) UNIQUE,
    enlace VARCHAR2(1500 CHAR),
    estado VARCHAR2(24 CHAR) NOT NULL,
    creado TIMESTAMP NOT NULL,
    vence TIMESTAMP NOT NULL,
    incidencia VARCHAR2(240 CHAR),
    actualizado TIMESTAMP
);

CREATE TABLE recibo_correo (
    pago_id NUMBER(19) PRIMARY KEY REFERENCES pago(id),
    destinatario VARCHAR2(254 CHAR) NOT NULL,
    estado VARCHAR2(16 CHAR) NOT NULL,
    intentos NUMBER(3) DEFAULT 0 NOT NULL,
    proximo TIMESTAMP NOT NULL,
    mensaje_externo VARCHAR2(120 CHAR)
);
CREATE INDEX recibo_envio_ix ON recibo_correo(estado, proximo);

ALTER TABLE vehiculo ADD eliminado NUMBER(1) DEFAULT 0 NOT NULL CHECK (eliminado IN (0,1));
CREATE INDEX pago_ingresos_ix ON pago(estado, revisado);
