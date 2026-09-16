-- SISTEMA DE CONTROL DE ASISTENCIA - BIENESTAR ESTUDIANTIL
-- Universidad Tecnica Estatal de Quevedo (UTEQ)
-- Dialecto : PostgreSQL


-- ELIMINACION EN ORDEN CORRECTO DE DEPENDENCIAS
DROP VIEW  IF EXISTS resumen_por_servicio        CASCADE;
DROP VIEW  IF EXISTS reporte_asistencias_diarias CASCADE;
DROP TABLE IF EXISTS log_accesos                 CASCADE;
DROP TABLE IF EXISTS turnos_diarios              CASCADE;
DROP TABLE IF EXISTS asistencias                 CASCADE;
DROP TABLE IF EXISTS disponibilidad_personal     CASCADE;
DROP TABLE IF EXISTS configuracion_sistema       CASCADE;
DROP TABLE IF EXISTS usuarios                    CASCADE;
DROP TABLE IF EXISTS servicios                   CASCADE;
DROP TABLE IF EXISTS areas                       CASCADE;
DROP TABLE IF EXISTS estudiantes                 CASCADE;
DROP TABLE IF EXISTS carreras                    CASCADE;
DROP TABLE IF EXISTS facultades                  CASCADE;
DROP TABLE IF EXISTS etnias                      CASCADE;
DROP TABLE IF EXISTS sexos                       CASCADE;


-- TABLA: sexos
CREATE TABLE sexos (
    id      SERIAL PRIMARY KEY,
    nombre  VARCHAR(20) UNIQUE NOT NULL,
    version BIGINT DEFAULT 0
);

-- TABLA: etnias
CREATE TABLE etnias (
    id      SERIAL PRIMARY KEY,
    nombre  VARCHAR(30) UNIQUE NOT NULL,
    version BIGINT DEFAULT 0
);

-- TABLA: facultades
CREATE TABLE facultades (
    id      SERIAL PRIMARY KEY,
    nombre  VARCHAR(150) UNIQUE NOT NULL,
    version BIGINT DEFAULT 0
);

-- TABLA: carreras
CREATE TABLE carreras (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    facultad_id INTEGER      NOT NULL REFERENCES facultades(id) ON DELETE RESTRICT,
    version     BIGINT DEFAULT 0,
    UNIQUE (nombre, facultad_id)
);

-- TABLA: estudiantes
CREATE TABLE estudiantes (
    id                   SERIAL PRIMARY KEY,
    cedula               VARCHAR(20)  UNIQUE NOT NULL,
    nombres              VARCHAR(100) NOT NULL,
    apellidos            VARCHAR(100) NOT NULL,
    correo_institucional VARCHAR(100) UNIQUE NOT NULL,
    carrera_id           INTEGER      NOT NULL REFERENCES carreras(id) ON DELETE RESTRICT,
    sexo_id              INTEGER      NOT NULL REFERENCES sexos(id)  ON DELETE RESTRICT,
    etnia_id             INTEGER      NOT NULL REFERENCES etnias(id) ON DELETE RESTRICT,
    fecha_registro       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    activo               BOOLEAN      DEFAULT TRUE,
    version              BIGINT       DEFAULT 0,

    CONSTRAINT chk_correo_institucional
        CHECK (correo_institucional ~ '^[^@\s]+@uteq\.edu\.ec$')
);

-- TABLA: areas
CREATE TABLE areas (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(100) UNIQUE NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN DEFAULT TRUE,
    version     BIGINT  DEFAULT 0
);

-- TABLA: servicios
CREATE TABLE servicios (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT,
    area_id     INTEGER      NOT NULL REFERENCES areas(id) ON DELETE RESTRICT,
    activo      BOOLEAN      DEFAULT TRUE,
    version     BIGINT       DEFAULT 0,
    UNIQUE (nombre, area_id)
);

-- TABLA: usuarios
CREATE TABLE usuarios (
    id             SERIAL PRIMARY KEY,
    usuario        VARCHAR(50)  UNIQUE NOT NULL,
    nombres        VARCHAR(100) NOT NULL,
    apellidos      VARCHAR(100) NOT NULL,
    correo         VARCHAR(100) UNIQUE NOT NULL,
    contrasena     VARCHAR(255) NOT NULL,
    rol            VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'ENCARGADO')),
    area_id        INTEGER      REFERENCES areas(id) ON DELETE SET NULL,
    activo         BOOLEAN      DEFAULT TRUE,
    fecha_registro TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    version        BIGINT       DEFAULT 0
);

-- TABLA: configuracion_sistema
CREATE TABLE configuracion_sistema (
    id                  INTEGER PRIMARY KEY DEFAULT 1,
    hora_cierre         TIME,
    fecha_ultimo_cierre DATE,
    version             BIGINT DEFAULT 0,
    CONSTRAINT chk_configuracion_fila_unica CHECK (id = 1)
);

INSERT INTO configuracion_sistema (id, hora_cierre, fecha_ultimo_cierre)
VALUES (1, NULL, NULL);

-- TABLA: log_accesos
CREATE TABLE log_accesos (
    id                   SERIAL PRIMARY KEY,
    usuario_id           INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha_hora           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_acceso            VARCHAR(45),
    notificacion_enviada BOOLEAN   DEFAULT FALSE,
    version              BIGINT    DEFAULT 0
);

-- TABLA: asistencias
CREATE TABLE asistencias (
    id                     SERIAL PRIMARY KEY,
    estudiante_id          INTEGER NOT NULL REFERENCES estudiantes(id) ON DELETE RESTRICT,
    servicio_id            INTEGER NOT NULL REFERENCES servicios(id)   ON DELETE RESTRICT,
    usuario_asignado_id    INTEGER REFERENCES usuarios(id) ON DELETE SET NULL,
    usuario_atendio_id     INTEGER REFERENCES usuarios(id) ON DELETE SET NULL,
    numero_turno           INTEGER,
    token_verificacion     VARCHAR(64),
    fecha_expiracion_token TIMESTAMP,
    fecha_hora_registro    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_hora_atencion    TIMESTAMP,
    fecha_hora_cierre      TIMESTAMP,
    ultimo_cambio_estado   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado                 VARCHAR(30) DEFAULT 'PENDIENTE_VERIFICACION'
                               CHECK (estado IN (
                                   'PENDIENTE_VERIFICACION',
                                   'ESPERANDO',
                                   'EN_ATENCION',
                                   'ATENDIDO',
                                   'NO_SE_PRESENTO',
                                   'CANCELADO'
                               )),
    observaciones          TEXT,
    es_primera_vez         BOOLEAN DEFAULT FALSE,
    version                BIGINT  DEFAULT 0,

    CONSTRAINT chk_cronologia_tiempos CHECK (
        (fecha_hora_atencion IS NULL OR fecha_hora_atencion >= fecha_hora_registro) AND
        (fecha_hora_cierre   IS NULL OR fecha_hora_cierre   >= fecha_hora_registro)
    ),

    CONSTRAINT chk_estado_coherencia CHECK (
        (estado = 'PENDIENTE_VERIFICACION'
            AND fecha_hora_atencion IS NULL
            AND fecha_hora_cierre   IS NULL)
        OR
        (estado = 'ESPERANDO'
            AND fecha_hora_atencion IS NULL
            AND fecha_hora_cierre   IS NULL)
        OR
        (estado = 'EN_ATENCION'
            AND fecha_hora_atencion IS NOT NULL
            AND fecha_hora_cierre   IS NULL)
        OR
        (estado = 'ATENDIDO'
            AND fecha_hora_atencion IS NOT NULL
            AND fecha_hora_cierre   IS NOT NULL)
        OR
        (estado = 'NO_SE_PRESENTO'
            AND fecha_hora_atencion IS NULL
            AND fecha_hora_cierre   IS NOT NULL)
        OR
        (estado = 'CANCELADO'
            AND fecha_hora_cierre IS NOT NULL)
    )
);

-- TABLA: turnos_diarios
CREATE TABLE turnos_diarios (
    id           SERIAL PRIMARY KEY,
    area_id      INTEGER NOT NULL REFERENCES areas(id) ON DELETE CASCADE,
    fecha        DATE    NOT NULL DEFAULT CURRENT_DATE,
    ultimo_turno INTEGER NOT NULL DEFAULT 0,
    version      BIGINT  DEFAULT 0,
    UNIQUE (area_id, fecha)
);

-- INDICES
CREATE INDEX idx_asistencias_fecha      ON asistencias (DATE(fecha_hora_registro));
CREATE INDEX idx_asistencias_servicio   ON asistencias (servicio_id);
CREATE INDEX idx_asistencias_estudiante ON asistencias (estudiante_id);
CREATE INDEX idx_asistencias_estado     ON asistencias (estado);
CREATE INDEX idx_log_accesos_usuario    ON log_accesos (usuario_id);
CREATE INDEX idx_token_verificacion     ON asistencias (token_verificacion)
                                        WHERE token_verificacion IS NOT NULL;
CREATE INDEX idx_estudiantes_sexo       ON estudiantes (sexo_id);
CREATE INDEX idx_estudiantes_etnia      ON estudiantes (etnia_id);
CREATE INDEX idx_turnos_area_fecha      ON turnos_diarios (area_id, fecha);
CREATE INDEX idx_usuarios_usuario       ON usuarios (usuario);

-- DATOS INICIALES - Sexo
INSERT INTO sexos (nombre) VALUES
    ('Masculino'),
    ('Femenino');

-- DATOS INICIALES - Etnia
INSERT INTO etnias (nombre) VALUES
    ('Mestizo'),
    ('Indígena'),
    ('Afroecuatoriano'),
    ('Montubio'),
    ('Blanco'),
    ('Otra');

-- DATOS INICIALES - Facultades UTEQ (9 facultades)
INSERT INTO facultades (nombre) VALUES
    ('Facultad de Ciencias Pecuarias y Biológicas'),
    ('Facultad de Ciencias Empresariales'),
    ('Facultad de Ciencias Sociales, Económicas y Financieras'),
    ('Facultad de Ciencias Agrarias y Forestales'),
    ('Facultad de Ciencias de la Industria y Producción'),
    ('Facultad de Ciencias de la Ingeniería'),
    ('Facultad de Ciencias de la Educación'),
    ('Facultad de Ciencias de la Computación'),
    ('Facultad de Ciencias de la Salud');

-- DATOS INICIALES - Carreras UTEQ (37 carreras)
INSERT INTO carreras (nombre, facultad_id) VALUES
    ('Acuicultura',  1), ('Agropecuaria', 1),
    ('Biología',     1), ('Zootecnia',    1),
    ('Administración de Empresas', 2), ('Contabilidad y Auditoría',  2),
    ('Gestión de Talento Humano',  2), ('Mercadotecnia',             2),
    ('Administración Pública', 3), ('Economía', 3),
    ('Finanzas',               3), ('Turismo',  3),
    ('Agroecología',        4), ('Agronomía',           4),
    ('Ingeniería Agrícola', 4), ('Ingeniería Forestal', 4),
    ('Agroindustria',         5), ('Alimentos',            5),
    ('Ingeniería Industrial', 5), ('Seguridad Industrial', 5),
    ('Arquitectura',         6), ('Electricidad',         6),
    ('Ingeniería Ambiental', 6), ('Ingeniería Civil',     6),
    ('Mecánica',             6),
    ('Educación',                                         7),
    ('Educación Básica',                                  7),
    ('Educación Inicial',                                 7),
    ('Pedagogía de los Idiomas Nacionales y Extranjeros', 7),
    ('Psicopedagogía',                                    7),
    ('Sistemas de Información',       8), ('Software',                      8),
    ('Tecnologías de la Información', 8), ('Telemática',                    8),
    ('Enfermería', 9);

-- DATOS INICIALES - Areas (5 areas)
INSERT INTO areas (nombre) VALUES
    ('Trabajo Social'),
    ('Psicología'),
    ('Danza'),
    ('Música'),
    ('Deportes');

-- DATOS INICIALES - Servicios (14 servicios)
INSERT INTO servicios (nombre, area_id) VALUES
    ('Becas y Ayudas Económicas',         1),
    ('Apoyo en Emergencias (Calamidades)',1),
    ('Consultas y Apoyo Emocional',               2),
    ('Evaluación de Dificultades de Aprendizaje', 2),
    ('Charlas de Salud Mental',                   2),
    ('Clases de Baile',                                   3),
    ('Participación en Obras y Presentaciones de Baile',  3),
    ('Clases para Aprender a Tocar Instrumentos Musicales y Canto', 4),
    ('Ingreso a los Grupos Musicales Oficiales',                     4),
    ('Participación en Conciertos y Recitales',                      4),
    ('Asesoría Deportiva',                 5),
    ('Organización de Eventos Deportivos', 5),
    ('Petición de Campo Deportivo',        5),
    ('Servicio de Arbitraje',              5);

-- DATOS INICIALES - Usuarios del sistema
INSERT INTO usuarios (usuario, nombres, apellidos, correo, contrasena, rol, area_id) VALUES
    ('admin', 'Administrador', 'Sistema',
     'admin@bienestar.edu.ec',
     '$2b$12$eaeFIT9M466d5I08hxXBbOEcSBwoDgZNNCYxbN5Y3/fFBhTQ4Sr52',
     'ADMIN', NULL),

    ('trabajo.social', 'Encargado', 'Trabajo Social',
     'trabajo.social@bienestar.ec',
     '$2b$12$kT.DL9pKGU./Sj74/6lYHuYEPgug1mT9CLB2I8v5ePcfQ09c8rMoW',
     'ENCARGADO', 1),

    ('psicologia', 'Encargado', 'Psicología',
     'psicologia@bienestar.ec',
     '$2b$12$hye4/21tHGcm.hUvDutrzelSPab2rT3aWdOVrp1OJr0nDvUqqrKNW',
     'ENCARGADO', 2),

    ('danza', 'Encargado', 'Danza',
     'danza@bienestar.ec',
     '$2b$12$7MQhWUjPXh8gBX3KsuWFjOD46KtYo6k8q6QPTpCdVVV3Qky2tmP7.',
     'ENCARGADO', 3),

    ('musica', 'Encargado', 'Música',
     'musica@bienestar.ec',
     '$2b$12$MniTiErjVMF4nc71T8XaReXiWQiJwQzUI8QQA0aQ2LSMbOrej18fm',
     'ENCARGADO', 4),

    ('deportes', 'Encargado', 'Deportes',
     'deportes@bienestar.ec',
     '$2b$12$6zTm3R6iDNuWAAJb/6otUuYrt5PHcgC66I1jXEPz4QpMGVP6Q/P5W',
     'ENCARGADO', 5);

-- VISTA: reporte_asistencias_diarias
CREATE OR REPLACE VIEW reporte_asistencias_diarias AS
SELECT
    a.id,
    DATE(a.fecha_hora_registro)          AS fecha,
    a.fecha_hora_registro                AS hora_registro,
    a.fecha_hora_atencion                AS hora_atencion,
    a.fecha_hora_cierre                  AS hora_cierre,
    a.numero_turno                       AS turno,
    e.cedula,
    e.nombres || ' ' || e.apellidos      AS estudiante,
    e.correo_institucional,
    sx.nombre                            AS sexo,
    et.nombre                            AS etnia,
    f.nombre                             AS facultad,
    c.nombre                             AS carrera,
    ar.nombre                            AS area,
    s.nombre                             AS servicio,
    ua.nombres  || ' ' || ua.apellidos   AS encargado_asignado,
    uat.nombres || ' ' || uat.apellidos  AS encargado_que_atendio,
    a.estado,
    a.es_primera_vez,
    a.observaciones
FROM asistencias a
JOIN estudiantes e    ON a.estudiante_id        = e.id
JOIN sexos       sx   ON e.sexo_id              = sx.id
JOIN etnias      et   ON e.etnia_id             = et.id
JOIN carreras    c    ON e.carrera_id           = c.id
JOIN facultades  f    ON c.facultad_id          = f.id
JOIN servicios   s    ON a.servicio_id          = s.id
JOIN areas       ar   ON s.area_id              = ar.id
LEFT JOIN usuarios ua  ON a.usuario_asignado_id = ua.id
LEFT JOIN usuarios uat ON a.usuario_atendio_id  = uat.id;

-- VISTA: resumen_por_servicio
CREATE OR REPLACE VIEW resumen_por_servicio AS
SELECT
    ar.nombre                            AS area,
    s.nombre                             AS servicio,
    DATE(a.fecha_hora_registro)          AS fecha,
    COUNT(*)                             AS total_registros,
    COUNT(*) FILTER (WHERE a.estado = 'ATENDIDO')       AS atendidos,
    COUNT(*) FILTER (WHERE a.estado = 'NO_SE_PRESENTO') AS no_se_presentaron,
    COUNT(*) FILTER (WHERE a.estado = 'CANCELADO')      AS cancelados,
    COUNT(*) FILTER (WHERE a.es_primera_vez = TRUE)     AS primera_vez
FROM asistencias a
JOIN servicios s ON a.servicio_id = s.id
JOIN areas    ar ON s.area_id     = ar.id
GROUP BY ar.nombre, s.nombre, DATE(a.fecha_hora_registro)
ORDER BY fecha DESC, total_registros DESC;

-- Reproducir: createdb unidad_bienestar_universitariov07; psql -d unidad_bienestar_universitariov07 -f docs/db/schema.sql
-- JPA ddl-auto=validate ahora pasa (areas, cedula, contrasena sin tildes)
