-- Asegúrate de estar en la PDB correcta
ALTER SESSION SET CONTAINER = FREEPDB1;

-- (Opcional) Verifica dónde estás
SELECT SYS_CONTEXT('USERENV','CON_NAME') AS con FROM dual;

-- Crea el usuario si no existe
CREATE USER una IDENTIFIED BY una QUOTA UNLIMITED ON USERS;

-- Permisos mínimos para trabajar
GRANT CREATE SESSION TO una;
GRANT CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE, CREATE TRIGGER TO una;

-- Por si acaso, desbloquea y fija clave
ALTER USER una IDENTIFIED BY una ACCOUNT UNLOCK;
