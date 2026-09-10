# Sistema de Bienestar Universitario - UTEQ

Sistema de gestion de turnos y atencion estudiantil para la Unidad de Bienestar Universitario (UBU) de la UTEQ. Spring Boot 3.5.14 / Java 21 / PostgreSQL / Thymeleaf / Spring Security.

## Requisitos

- Java 21
- Maven 3.9+ (incluido wrapper `./mvnw`)
- PostgreSQL 15+
- Cuenta Gmail con App Password (para notificaciones)

## Reproducir en local (clone -> run)

```bash
git clone <url-del-repo>
cd sistema-bienestar-universitario

# 1. Variables de entorno (NUNCA subir .env real)
cp .env.example .env
# Edita .env con tu postgres y tu App Password de Gmail

# 2. Base de datos
createdb unidad_bienestar_universitariov07
psql -d unidad_bienestar_universitariov07 -f docs/db/schema.sql

# 3. Ejecutar
./mvnw spring-boot:run        # Linux/Mac
.\mvnw.cmd spring-boot:run    # Windows
# App en http://localhost:8080

# 4. Tests
./mvnw test
```

### Configurar correo Gmail

1. `myaccount.google.com > Seguridad > Verificacion en 2 pasos` activada
2. `Seguridad > Contrasenas de aplicaciones > Generar` (16 caracteres)
3. Pega en `.env` como `MAIL_PASSWORD` sin espacios

## Estructura

```
src/main/java/com/bienestar/.../model/       # 12 entidades JPA
src/main/java/.../repository/                # 12 repos Spring Data
src/main/java/.../service/                   # 9 servicios (Turno, Asistencia, Reportes, Correo...)
src/main/java/.../config/                    # SecurityConfig, WebConfig, RateLimit
src/main/java/.../controller/                # Root, Estudiante, Auth, Admin, Encargado
src/main/resources/templates/                # Thymeleaf (admin, encargado, estudiante)
src/main/resources/static/                   # css, img, fonts
src/test/java/                               # MockMvc ReportesTests, EncargadoVistasTests
docs/db/schema.sql                           # Esquema y datos semilla
docs/organigrama-ubu.png                     # Anexo
```

## Roles

- **ANONIMO/ESTUDIANTE**: `GET /`, `/estudiante/registro`, `POST /estudiante/registrar`
- **ADMIN**: `/admin/**` (dashboard, hora cierre, usuarios, reportes excel/pdf)
- **ENCARGADO**: `/encargado/**` (panel cola FIFO, atender/finalizar, reporte)

## Variables de entorno (.env)

Ver `.env.example`. `application.properties` usa `${DB_USERNAME}`, `${MAIL_PASSWORD}` via `spring-dotenv`.

## Licencia

Uso academico UTEQ.
