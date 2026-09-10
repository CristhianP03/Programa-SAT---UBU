-- PLACEHOLDER - REEMPLAZAR POR DUMP REAL
-- Base: unidad_bienestar_universitariov07
-- Generar con: pg_dump -U postgres -d unidad_bienestar_universitariov07 --schema-only > docs/db/schema.sql
-- Luego: pg_dump -U postgres -d unidad_bienestar_universitariov07 --data-only --inserts >> docs/db/schema.sql
-- O dump completo: pg_dump -U postgres -d unidad_bienestar_universitariov07 > docs/db/schema.sql
--
-- Este archivo es OBLIGATORIO para reproducir. application.properties usa ddl-auto=validate.
-- Sin este dump, git clone + ./mvnw spring-boot:run fallara con "relation does not exist".
--
-- Tablas esperadas (ver model/): area, facultad, carrera, servicio, estudiante, sexo, etnia,
-- usuario, asistencia, turno_diario, configuracion_sistema, log_acceso
--
-- EJEMPLO MINIMO (reemplazar por dump real):
-- CREATE TABLE area (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(100) NOT NULL, activo BOOLEAN);
-- CREATE TABLE facultad (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(100), area_id BIGINT REFERENCES area(id));
-- ...

SELECT 'REEMPLAZAR ESTE PLACEHOLDER POR pg_dump REAL' as advertencia;
