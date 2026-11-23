ALTER TABLE tipo_clase ADD eliminado NUMBER(1) DEFAULT 0 NOT NULL CHECK (eliminado IN (0,1));
ALTER TABLE usuario ADD eliminado NUMBER(1) DEFAULT 0 NOT NULL CHECK (eliminado IN (0,1));
