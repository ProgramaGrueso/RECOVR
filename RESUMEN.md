# RESUMEN — trabajo por fases

Cinco fases, cada una en su rama y con su PR a `main`. Los PR se **apilan**, así que conviene fusionarlos en orden:

| Fase | Rama | PR |
|---|---|---|
| 1. Base de datos con historial | `fase/1-datos` | #43 |
| 2. Bugs | `fase/2-bugs` | #44 |
| 3. Refactorización | `fase/3-refactor` | #45 |
| 4. Escalado | `fase/4-escalado` | #46 |
| 5. Seguridad | `fase/5-seguridad` | #47 |

Estado final: **backend 165 pruebas en verde** (antes 120), **frontend 40 pruebas en verde** (antes 28), `ng build` OK y `npm audit` con 0 vulnerabilidades. `vercel.json` conserva el mismo build, la misma salida y los mismos rewrites; solo se le añadieron cabeceras de seguridad.

---

## Fase 1 — Base de datos con 3 meses de historial (datos 100 % ficticios)
- Perfil Spring **`demo`**: deja la base como si el sistema llevara **90 días en uso** y con citas hasta el **31 de diciembre**.
- `GeneradorAgendaDemo` es determinista. De lunes a sábado hay más carga (viernes y sábado los días más llenos) y el domingo casi no hay citas (0 a 3). Las horas pico son las primeras tras la apertura (12:30–14:00; el negocio abre a mediodía, así que la "media mañana" se movió a esa franja) y la tarde-noche (17:30–20:00). En el futuro cada día es lleno, medio o tranquilo.
- Sin choques por terapeuta, cabina ni paciente, y siempre dentro del horario 12:00–23:00.
- Estados: en el pasado, completadas, canceladas y "no asistió"; en el futuro, confirmadas y pendientes. Los pagos son coherentes.
- 140 pacientes con nombres peruanos **inventados**, correo `@example.com`, teléfono `900000xxx` y sin DNI.
- **Re-ejecutable sin duplicar**: verificado sobre MySQL 8 (la segunda ejecución crea 0 citas nuevas).
- Pruebas `DemoDataSeederTest` (incluye **sin citas solapadas**) y `GeneradorAgendaDemoTest`, más `bdd/verificar-agenda.sql`.

## Fase 2 — Bugs (cada uno con una prueba que fallaba antes de corregirlo)
1. **Zona horaria (backend):** las reglas usaban la zona de la JVM. Ahora un `Clock` en America/Lima en todos los servicios.
2. **Reprogramar sin cabina** respondía 409 aunque hubiera otra cabina libre.
3. **Bloqueos concurrentes** (lock wait) respondían 500: ahora 409. Los errores inesperados devuelven un 500 genérico, sin detalles internos.
4. **Textos sin longitud máxima** acababan en un 409 "duplicado" falso: ahora 400 con el campo.
5. **Zona horaria (frontend):** el "hoy" y la hora del panel salían de la zona del navegador.
6. **Fuga de listeners** de scroll en el hero.
7. **Carrera en la consulta de turnos:** una respuesta vieja pisaba la nueva.
8. **"Reservar otra"** dejaba la fecha vacía.
9. Aviso de `THREE.Clock` obsoleto en la consola.

## Fase 3 — Refactorización (sin cambios visibles)
- `ReservaService` (~600 líneas) se dividió en `ValidadorAgenda`, `DisponibilidadService` y `UsuarioActualService`. Se eliminó código muerto y reglas duplicadas.
- Formato de error único (`CuerpoError`) para el `@RestControllerAdvice` y los filtros de seguridad.
- `CatalogoSeeder` quedó separado de los datos demo.
- Frontend: `ApiService` sin duplicación y con consultas en paralelo; filtrado de la agenda en una función pura con pruebas; plantillas sin lógica; sin `as any`.

## Fase 4 — Escalado
- Índices en fecha, terapeuta, cabina, cliente y estado.
- **N+1 eliminado**: un listado pasó de 305–470 consultas SQL a 1–3.
- **Paginación** (`desde`, `hasta`, `pagina`, `tamano`; cabeceras `X-Total-Count` y `X-Total-Pages`); el panel pide solo su ventana.
- Caché Caffeine del catálogo, pool HikariCP configurable y gzip.
- Configuración **100 % por variables de entorno** y perfiles **dev / demo / prod**.
- `Dockerfile` multietapa y `docker-compose` con réplicas, un planificador único y nginx. Verificado que el backend es **stateless** (un token emitido por una réplica vale en la otra).
- Medido (detalle en `backend/perf/RESULTADOS.md`): el listado de la agenda pasó de **7 a 72 req/s** (p50 de 3 s a 0,27 s) y, paginado, a **346 req/s**.

## Fase 5 — Seguridad (detalle en `SECURITY_REPORT.md`)
- `DEMO_USERS` ya no existía (verificado); el login siempre va contra el backend.
- CORS con **lista blanca** (`CORS_ALLOWED_ORIGINS`).
- Fuera del código y de git: las **contraseñas fijas de las cuentas demo** (ahora `DEMO_PASSWORD`; las antiguas se desactivan), la clave JWT de las pruebas y `root/root`. Hay un `.env.example` sin valores.
- Autorización explícita en cada ruta y **denegar por defecto**.
- JWT ligado a la contraseña vigente (cambiarla invalida los tokens anteriores), emisor verificado y 8 h de vida.
- **Rate limiting** por IP en login y registro, con la IP real detrás del balanceador; login con tiempo de respuesta uniforme.
- Cabeceras de seguridad en la API y en Vercel.
- Dependencias: Spring Boot 3.3.5 (sin soporte) → **3.5.16**; npm de **18 vulnerabilidades a 0**. Dependabot y un perfil de OWASP Dependency-Check.

---

## Cómo levantar todo con el seeder

Requisitos: Java 21, Maven 3.9+, Node 22 y Docker.

```bash
# 1) Configuración (una vez)
cd backend
cp .env.example .env
#   Completa en .env: DB_ROOT_PASSWORD, DB_PASSWORD, JWT_SECRET (openssl rand -base64 48),
#   DEMO_PASSWORD (mín. 10 caracteres con letras y números) y, si quieres, ADMIN_EMAIL / ADMIN_PASSWORD.

# 2) Base de datos + backend con 3 meses de datos ficticios
./run-demo.sh                 # Linux/macOS  (Windows: .\run-demo.ps1)
#   -> API en http://localhost:8095   ·   Swagger: http://localhost:8095/swagger-ui/index.html

# 3) Frontend (otra terminal)
cd ../frontend
npm ci
npm start                     # http://localhost:4200  ·  acceso: http://localhost:4200/admin/login
#   Entra con admin.demo@example.com (o recepcion.demo, ely.demo, cliente.demo...) y la clave de DEMO_PASSWORD.
```

Todo en contenedores, con 2 réplicas del API detrás de nginx (pon `SEED_DEMO_DATA=true` en `.env` para sembrar):
```bash
cd backend
docker compose --profile app up -d --build --scale api=2
```

Verificar la agenda sembrada y medir el rendimiento:
```bash
docker exec -i recovr-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" recovr_db' < bdd/verificar-agenda.sql
API=http://localhost:8095/api CORREO=admin.demo@example.com CLAVE="$DEMO_PASSWORD" node perf/prueba-carga.mjs
```

Pruebas:
```bash
cd backend && mvn test
cd frontend && npx ng test --watch=false && npx ng build
```

---

## Qué quedó pendiente
1. **Historial de git:** las contraseñas demo antiguas, una clave JWT por defecto antigua y `DEMO_USERS` siguen en commits viejos (la regla es no reescribir el historial). Hay que darlas por comprometidas: no reutilizarlas, rotar `JWT_SECRET` si algún entorno usó la antigua y borrar el archivo local `backend/CREDENCIALES_DEMO.md`, que nunca se subió a git y ya no se necesita.
2. **Escaneo NVD de dependencias Maven:** no se pudo correr en este entorno porque el proxy bloquea NVD y OSV. Correr `mvn -P seguridad verify -DnvdApiKey=…` en CI o en un equipo con Internet.
3. **Límites en memoria por instancia** (login y chat de IA): con varias máquinas, llevarlos a Redis o a la base.
4. **CSP para el frontend** en `vercel.json`: requiere inventariar los scripts y estilos inline, Three.js, los videos y el iframe.
5. **JWT en `localStorage`:** migrarlo a una cookie `HttpOnly` implicaría CSRF y cambios de dominio.
6. **Open-Session-In-View** sigue activo de forma explícita. Los listados ya no tienen N+1; para desactivarlo hay que armar los DTO dentro de transacciones (por ejemplo en `cancelar`, que usa `SELECT … FOR UPDATE` sin JOIN).
7. **Migraciones de esquema:** se usa `ddl-auto=update`. Para producción conviene Flyway o Liquibase (`bdd/recovr.sql` está desactualizado respecto de las entidades).
8. **Planificador vs. acciones del usuario:** el planificador no bloquea filas al expirar o marcar no-show. Una carrera con un cobro simultáneo es posible, aunque poco probable.
9. **Escalado horizontal real:** en una sola máquina de 4 vCPU la segunda réplica no suma rendimiento (el límite es la CPU). La ganancia se mide con réplicas en máquinas distintas.
10. En este sandbox la imagen Docker se construyó con la etapa de ejecución y el JAR compilado en el host, porque Maven dentro de Docker no confía en la CA del proxy. En un equipo normal, `docker compose build` usa las dos etapas del `Dockerfile`.
