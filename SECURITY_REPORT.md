# Informe de seguridad — RECOVR

**Alcance:** backend (Spring Boot), frontend (Angular), configuración, Docker y despliegue (`vercel.json`), historial de git.
**Método:** revisión manual del código, pruebas automáticas nuevas (cada corrección tiene una prueba que antes fallaba), `npm audit`, comparación de las versiones Maven resueltas con los avisos de seguridad publicados y pruebas en vivo sobre MySQL 8 con 2 réplicas detrás de nginx.
**Rama:** `fase/5-seguridad`.

Escala de gravedad: **Crítica** (compromiso directo y trivial), **Alta**, **Media**, **Baja** e **Info**.

## Resumen

| # | Hallazgo | Gravedad | Estado |
|---|----------|----------|--------|
| 1 | Contraseñas fijas de las 11 cuentas demo (ADMIN incluida) escritas en el código de un repo público | Alta | ✅ Corregido · ⚠️ siguen en el historial de git |
| 2 | Clave JWT por defecto versionada en el pasado y clave fija en las pruebas | Alta | ✅ Corregido · ⚠️ la antigua sigue en el historial |
| 3 | `DEMO_USERS` con contraseñas en `auth.service.ts` | Alta | ✅ Ya eliminado antes de esta fase (verificado) · ⚠️ sigue en el historial |
| 4 | CORS aceptaba cualquier `*.vercel.app` y cualquier puerto de `localhost`; todo preflight respondía 200 | Media | ✅ Corregido |
| 5 | Rutas sin regla explícita abiertas a cualquier rol autenticado (`anyRequest().authenticated()`) | Media | ✅ Corregido |
| 6 | El token seguía valiendo hasta 24 h después de cambiar o restablecer la contraseña | Media | ✅ Corregido |
| 7 | Rate limiting solo por correo+IP (permitía *credential stuffing*), registro sin límite y, detrás de un balanceador, una sola IP para todos | Media | ✅ Corregido |
| 8 | Dependencias con vulnerabilidades: npm (3 críticas, 9 altas) y Spring Boot 3.3.5 sin soporte | Media | ✅ Corregido · ⚠️ falta correr el escaneo NVD en CI |
| 9 | `docker-compose` con `root/root` publicado en el README y la app conectada como `root` | Media | ✅ Corregido |
| 10 | Faltaban cabeceras de seguridad (CSP, Referrer-Policy, Permissions-Policy) | Baja | ✅ API y Vercel · ⚠️ falta una CSP para el frontend |
| 11 | El tiempo de respuesta del login revelaba si un correo estaba registrado | Baja | ✅ Corregido |
| 12 | Textos sin longitud máxima (409 "registro duplicado" falso) y errores 500 con el formato por defecto | Baja | ✅ Corregido en la Fase 2 |
| 13 | Teléfonos de las cuentas demo que podían pertenecer a personas reales | Baja | ✅ Corregido |
| 14 | El JWT se guarda en `localStorage` | Baja | ⚠️ Pendiente (decisión de diseño) |
| 15 | Límites de login y del chat de IA en memoria, por instancia | Baja | ⚠️ Pendiente |
| 16 | El registro responde "Ya existe un usuario con ese correo" (enumeración) | Info | Aceptado (mitigado con el límite por IP) |
| — | Inyección SQL | — | Sin hallazgos |
| — | XSS | — | Sin hallazgos |
| — | Hash de contraseñas | — | Correcto (BCrypt) |
| — | Validación de entrada (Bean Validation) | — | Correcta tras la Fase 2 |

## Detalle

### 1. Contraseñas fijas de las cuentas demo — Alta
`DemoDataSeeder.CUENTAS` tenía la contraseña de cada cuenta escrita en el código (`Admin#Recovr2026`, `Recep#Recovr2026`…). El repositorio es público: cualquiera podía entrar como **ADMIN** en cualquier instancia arrancada con datos demo.
**Corrección:** las cuentas demo usan `DEMO_PASSWORD` (variable de entorno, mínimo 10 caracteres con letras y números). Sin esa variable no se crea ninguna y las que existieran **se desactivan**. Las del dominio antiguo `@recovr.test`, cuyas claves están en el historial, se desactivan siempre.
Pruebas: `DemoSinClaveTest`, `DemoDataSeederTest`. Verificado en vivo: la clave antigua da 401 y `DEMO_PASSWORD` da 200.
**Pendiente:** las claves antiguas siguen en el historial de git, que no se reescribe por regla del proyecto. Hay que darlas por comprometidas: no reutilizarlas en ningún sistema y borrar `backend/CREDENCIALES_DEMO.md` de los equipos del grupo. Ese archivo nunca se subió a git (verificado), pero contiene esas claves y ya no hace falta.

### 2. Clave JWT por defecto — Alta
En el commit `960c8dc` la propiedad era `app.jwt.secret=${JWT_SECRET:<clave en Base64>}`: sin la variable, cualquier instancia firmaba con una clave publicada, y con ella cualquiera podía fabricar un token ADMIN válido. Esa línea ya no estaba en `main`, pero las pruebas seguían con una clave fija.
**Corrección:** ningún valor por defecto. En `prod` falta `JWT_SECRET` → no arranca (`JwtServiceTest`). En `dev`, sin variable, se genera una clave aleatoria en memoria. Las pruebas generan una clave aleatoria en cada ejecución.
**Pendiente:** si algún entorno llegó a usar la clave antigua, hay que rotar `JWT_SECRET`.

### 3. `DEMO_USERS` en el frontend — Alta (ya resuelto)
Se eliminó en `34c9280`, antes de esta auditoría. Se verificó que no quedan usuarios ni contraseñas en el frontend: `AuthService.login` siempre llama a `/api/auth/login` y la sesión se confirma con `/api/auth/me` (cubierto por `auth.service.spec.ts`). El README todavía mencionaba `DEMO_USERS`; ya se corrigió.

### 4. CORS — Media
`WebCorsFilter` aceptaba por expresión regular `https://*.vercel.app`, es decir, el proyecto de cualquier persona en Vercel, y cualquier puerto de `localhost`. Además respondía 200 a todo preflight.
**Corrección:** lista blanca en `CORS_ALLOWED_ORIGINS` con orígenes exactos o patrones explícitos (por ejemplo `https://recovr-*.vercel.app`); `*` no se acepta. Un origen fuera de la lista no recibe cabeceras y su preflight responde **403**. En `prod` la variable es obligatoria. Prueba: `CorsListaBlancaTest`.

### 5. Autorización por rol en cada endpoint — Media
Se revisaron todos los endpoints (unos 80). Todos tenían una regla salvo `/api/auth/me` y `/api/auth/cambiar-clave`, que caían en `anyRequest().authenticated()`. El riesgo real era otro: un endpoint nuevo que se olvidara en `SecurityConfig` quedaba abierto a **cualquier rol**, igual que los endpoints de actuator.
**Corrección:** esas dos rutas tienen ahora su propia regla y el resto se rechaza con `anyRequest().denyAll()`. Prueba: `DenegarPorDefectoTest`. La matriz por rol ya existía y la verifica `SeguridadRolesTest`.

### 6. Expiración y validación del JWT — Media
El token se validaba bien (firma HS256, expiración y usuario activo), pero seguía vigente **24 h** aunque el usuario cambiara su contraseña o un ADMIN se la restableciera. Quien hubiera robado un token conservaba el acceso.
**Corrección:**
- El token lleva una huella (SHA-256 truncado) del hash de la contraseña vigente; al cambiarla, todos los tokens anteriores dejan de valer.
- Se firma y se exige el emisor `recovr-api`.
- La vida por defecto es de 8 h (`JWT_EXPIRATION_MS`).
- `/api/auth/cambiar-clave` devuelve un token nuevo, que el panel guarda.

Pruebas: `TokenTrasCambioDeClaveTest` y `admin-api.service.spec.ts`.

### 7. Rate limiting en el login — Media
Ya existía un bloqueo de 5 intentos por par correo+IP durante 5 minutos. Faltaba:
- **Límite por IP:** 20 fallos en 10 min contra cualquier correo → 429. Frena el *credential stuffing*.
- **Registro:** 5 cuentas por hora y por IP → 429, auditado.
- **IP real detrás del balanceador:** `server.forward-headers-strategy=native`. Sin esto, todos los clientes compartían la IP de nginx y un atacante podía bloquear a todo el mundo. Tomcat solo acepta `X-Forwarded-For` de proxies de red interna. Verificado en vivo: la auditoría registra la IP del cliente, no la de nginx.

Prueba: `LimitesPorIpTest`.

### 8. Dependencias con CVE conocidos — Media
**npm (frontend):** `npm audit` reportaba **18 vulnerabilidades (3 críticas, 9 altas, 6 moderadas)** en `@angular/build`, `@angular/cli`, `@angular/router`, `vite`, `undici`, `piscina`, `proxy-addr`, `zod` y otros. Se actualizó a Angular 22.2.4/22.2.5 y vitest 4.1.12: **0 vulnerabilidades**. El build y las pruebas siguen pasando, también con `npm ci` de npm 10, el que usan CI y Vercel.

**Maven (backend):** Spring Boot 3.3.5 pertenece a una línea **sin parches OSS desde junio de 2025**. Se actualizó a **3.5.16**:

| Componente | Antes | Después | Avisos publicados que afectaban a la versión anterior (ejemplos) |
|---|---|---|---|
| Tomcat embebido | 10.1.31 | 10.1.55 | CVE-2024-50379 / CVE-2024-56337, CVE-2025-24813, CVE-2024-52316 |
| Spring Security | 6.3.4 | 6.5.11 | CVE-2024-38827, CVE-2025-22228 (BCrypt con más de 72 bytes) |
| Spring Framework | 6.1.14 | 6.2.19 | CVE-2024-38828, CVE-2025-22233 |
| Spring Boot | 3.3.5 | 3.5.16 | CVE-2025-22235 (matcher de actuator) |
| logback | 1.5.11 | 1.5.34 | CVE-2024-12798, CVE-2024-12801 |
| Hibernate / Jackson / Connector-J / swagger-ui | 6.5 / 2.17 / 8.3 / 5.17 | 6.6 / 2.21 / 9.7 / 5.32 | parches varios |

**Pendiente:** el escaneo con OWASP Dependency-Check no se pudo ejecutar aquí porque el proxy del entorno bloquea el NVD y OSV. Quedó listo para correrlo en otro equipo o en CI:
```bash
cd backend && mvn -P seguridad verify -DnvdApiKey=$NVD_API_KEY   # falla con CVSS >= 7
```
Además se configuró **Dependabot** (`.github/dependabot.yml`) para Maven, npm y GitHub Actions.

### 9. Secretos en la configuración — Media
- `docker-compose.yml` tenía `MYSQL_ROOT_PASSWORD: root`, el healthcheck incluía `-proot` y el README publicaba "usuario root, contraseña root".
- `application.properties` usaba `${DB_PASSWORD:root}` por defecto.

**Corrección:** todo sale de `backend/.env`, que está en `.gitignore`. Hay un `.env.example` con las claves y sin valores. MySQL crea un **usuario propio de la aplicación** (no root) con permisos solo sobre `recovr_db`. El perfil `prod` no tiene ningún valor por defecto. `run-demo.sh` y `run-real.sh` cargan `.env`. Se revisó todo el árbol en busca de claves: no queda ninguna (la de DeepSeek siempre vino del entorno).

### 10. Cabeceras de seguridad — Baja
- **API:** `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` en `/api/**` (Swagger queda fuera), `Referrer-Policy: no-referrer`, `Permissions-Policy`, `X-Frame-Options: DENY` y HSTS de 1 año; Spring Security ya añadía `nosniff` y `no-cache`. Prueba: `CabecerasSeguridadTest`.
- **Frontend (`vercel.json`):** `nosniff`, `Referrer-Policy`, `X-Frame-Options: SAMEORIGIN` (la carita del asistente es un iframe propio), `Permissions-Policy` con micrófono solo para el propio sitio (dictado por voz) y HSTS. No se tocaron el build, la salida ni los rewrites.
- **Pendiente:** una CSP para el frontend. Requiere inventariar los estilos inline, Three.js, los videos y el iframe para no romper la experiencia visual.

### 11. Enumeración por tiempo en el login — Baja
Con un correo inexistente no se ejecutaba BCrypt, así que la respuesta era más rápida y delataba qué correos existían. Ahora se compara contra un hash señuelo y el mensaje sigue siendo el mismo.

### 12. Validación de entrada y mensajes de error — Baja
- Todos los `@RequestBody` llevan `@Valid` y DTOs con `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Positive` y similares. Ninguna entidad JPA se recibe directamente, así que no hay asignación masiva.
- En la Fase 2 se añadieron longitudes máximas (`ValidacionEntradaTest`) y un manejador de último recurso: los errores inesperados responden 500 con un mensaje genérico, sin clases ni mensajes internos (`ErroresApiTest`), y `server.error.include-stacktrace/message=never`.
- El chat de IA valida su cuerpo a mano: rol, longitud y número de mensajes.

### 13. Datos personales — Baja
Los teléfonos de las cuentas demo (`987654321`, `98700000x`) podían ser números reales y pasaron a la serie de ficción `900200xxx`. Los pacientes demo son ficticios: nombres combinados al azar, correo `@example.com` (RFC 2606), teléfono `900000xxx` y sin DNI.

### 14. JWT en `localStorage` — Baja (pendiente)
Si alguna vez existiera un XSS, el token se podría leer. Hoy no hay vectores XSS conocidos (ver abajo) y la vida del token es de 8 h. Moverlo a una cookie `HttpOnly; Secure; SameSite=Strict` exigiría protección CSRF y cambios en el despliegue con dominios separados (Vercel ↔ API), así que queda como mejora.

### 15. Límites en memoria por instancia — Baja (pendiente)
`LoginAttemptService` e `IaLimitador` cuentan en memoria: con N réplicas el límite efectivo es N veces mayor y se reinicia al reiniciar. Para producción con varias máquinas conviene un contador compartido, en Redis o en una tabla.

### 16. Enumeración en el registro — Info (aceptado)
`/api/auth/registro` responde "Ya existe un usuario con ese correo". Se acepta por usabilidad y queda mitigado con el límite de 5 registros por hora y por IP.

## Revisiones sin hallazgos

- **Inyección SQL:** todas las consultas son JPQL con parámetros nombrados o métodos derivados de Spring Data. No hay SQL nativo, `createQuery` dinámico ni concatenación de datos del usuario.
- **XSS:** Angular escapa la interpolación; no se usa `innerHTML` ni `eval`. El único `bypassSecurityTrustResourceUrl` recibe una URL constante (`/cara/avatar.html`) y su `postMessage` valida `event.origin`. La API solo devuelve JSON (con CSP `default-src 'none'`) e imágenes re-codificadas (sin metadatos).
- **Hash de contraseñas:** BCrypt (coste 10) en el registro, el primer admin, las cuentas creadas por ADMIN, el cambio de clave y las cuentas demo. Spring Security 6.5 rechaza contraseñas de más de 72 bytes (CVE-2025-22228) y la política exige 8 a 72 caracteres con letras y números.
- **Sesión:** stateless (`SessionCreationPolicy.STATELESS`), sin cookies; CSRF no aplica porque el token viaja en la cabecera `Authorization`. Una cuenta desactivada pierde el acceso al instante.
- **Superficie expuesta:** Swagger apagado en `prod` (`SWAGGER_ENABLED`) y de actuator solo `/actuator/health`, sin detalles. El resto se deniega.

## Cómo verificar

```bash
cd backend && mvn test                     # 165 pruebas; las de seguridad están en src/test/java/.../security
cd frontend && npm audit && npx ng test --watch=false
```
