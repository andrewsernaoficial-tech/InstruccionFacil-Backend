-- En SQL Developer, ejecutar dentro de la PDB de destino con una cuenta administradora.
-- Verificar el contenedor y sustituir los marcadores antes de ejecutar el CREATE USER.
SELECT SYS_CONTEXT('USERENV', 'CON_NAME') AS contenedor,
       SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS servicio
FROM dual;

CREATE USER instruccionfacil IDENTIFIED BY "<CONTRASENA_PROPIA>"
    DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA 250M ON USERS;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO instruccionfacil;
