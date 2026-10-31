# RECOVR Sanctum — Plataforma de Gestión y Experiencia Sensorial de Alta Gama

[![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6.x_JWT-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)](https://spring.io/projects/spring-security)
[![Angular](https://img.shields.io/badge/Angular-19%2F22_Standalone-DD0031?style=for-the-badge&logo=angular&logoColor=white)](https://angular.dev/)
[![Three.js](https://img.shields.io/badge/Three.js-WebGL_3D-000000?style=for-the-badge&logo=three.js&logoColor=white)](https://threejs.org/)
[![Node.js](https://img.shields.io/badge/Node.js-%3E%3D22.0.0-339933?style=for-the-badge&logo=node.js&logoColor=white)](https://nodejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker_Compose-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Vercel](https://img.shields.io/badge/Vercel-Deployed-000000?style=for-the-badge&logo=vercel&logoColor=white)](https://vercel.com/)

**RECOVR** es una solución digital integral de alta fidelidad diseñada para centros de bienestar somatosensorial, spas exclusivos y cabinas privadas de relajación. Combina una arquitectura de software robusta, desacoplada y orientada a micro-módulos con una interfaz web galardonada de estética **Dark Luxury / Vaporwave Bubblegum**, impulsada por renderizado 3D en WebGL, física elástica y micro-interacciones cinematográficas fluidas a 60 FPS estables.

---

## 🏛️ Arquitectura del Monorepo

El repositorio implementa una arquitectura desacoplada organizada en cuatro espacios fundamentales:

```
RECOVR/
├── backend/                  # API REST empresarial (Java 21 + Spring Boot 3.3.5)
│   ├── src/main/java/        # Controladores, Servicios, Repositorios, DTOs y Seguridad JWT
│   ├── src/test/java/        # Suite de pruebas unitarias e integración (JUnit 5 + Mockito)
│   ├── docker-compose.yml    # Orquestación de base de datos MySQL 8 local
│   └── pom.xml               # Configuración Maven y dependencias del sistema
├── frontend/                 # Aplicación SPA interactiva (Angular Standalone + Three.js)
│   ├── src/app/              # Componentes standalone, páginas, servicios y directivas
│   ├── src/styles/           # Tokens de diseño, mixins y temas Bubblegum/Dark Luxury
│   ├── public/               # Assets WebP optimizados, clips H.264 y Lab Awwwards (/PRUEBAS/)
│   ├── GUIA_REPLICACION_FRONTEND.md # Manual maestro de replicación de efectos y rendimiento
│   └── ARQUITECTURA.md       # Arquitectura técnica editorial y análisis de benchmarking
├── Prototipo/                # Laboratorio experimental de diseño y animación de vanguardia
│   ├── PRUEBAS/              # 5 arquetipos interactivos de alta gama (Awwwards Style)
│   ├── src/                  # Prototipo en Vite + Vanilla TypeScript + Lenis Smooth Scroll
│   └── vite.config.js        # Empaquetado multipágina de prototipado rápido
├── exposicion/               # Expedientes académicos, sustentaciones, APFs y guías de roles
│   ├── avanze1/              # APF1 corregido, mapas de API, diapositivas y guías de integrantes
│   ├── avanze2/              # Entregable y avances de desarrollo
│   ├── avanze3/              # Sustentación final del proyecto integrado
│   └── incidente/            # Reportes de post-mortem y resolución de incidentes técnicos (CI/CD)
├── .github/workflows/        # Pipelines automatizados de CI en GitHub Actions (Java 21 & Node 22)
├── vercel.json               # Configuración de despliegue optimizado para monorepo
└── package.json              # Scripts orquestadores en la raíz
```

---

## ✨ Características Principales

### 1. Frontend: Inmersión Visual & Rendimiento Extremo
- **Cursor Gatito (traído de Wimbledon):** Maxwell the Cat girando y balanceándose sigue al mouse con un resorte corto, se inclina según la velocidad, deja una estela de burbujas rosa/lila/cian y muestra la etiqueta contextual de los elementos con `[data-cursor]`. Solo con mouse real (no en táctiles); con *reducir movimiento* queda quieto y sin estela.
- **Efecto de Inclinación en Perspectiva 3D (`[appCardTilt]`):** Directiva Angular optimizada que calcula dinámicamente la posición del puntero y aplica transformaciones tridimensionales (`perspective`, `rotateX`, `rotateY`) en tiempo real ejecutadas 100% fuera de los ciclos de `NgZone`.
- **Atmósfera Somatosensorial con Three.js:** Lienzo WebGL con generación procedural de volutas de humo suave, luces de color neón rosa/púrpura/cian y ascuas luminosas ascendentes (*stardust*), optimizado con carga diferida `@defer (on viewport)` e `IntersectionObserver` para consumo 0% de GPU cuando no está en pantalla.
- **Reproducción de Video con Audio Inteligente en Hover:** Previsualizaciones de video en tarjetas de masajes y terapeutas con protocolo de evasión de restricciones de *Autoplay Policy* de los navegadores (fallback automático a silencioso sin arrojar excepciones).
- **Carrusel Interactivo de Tratamientos:** la carta se recorre como las habitaciones de Wimbledon: tarjetas llave sobre una pantalla CRT con el tratamiento central como foco (reproduce su video), filtros por categoría (`TODOS`, `TÁNTRICO`, `NURU BODY SLIDE`, `DESCONTRACTURANTE`, `4 MANOS`, `VIP HIDROMASAJE`), flechas del teclado, clic o deslizamiento, modo B&N y botones *Ver tratamiento* / *Reservar*.
- **Ositos RECOVR:** sección antiestrés con ositos de gelatina 3D (Three.js) que se estiran, aplastan y lanzan (`public/gummy/`), dentro de la portada.
- **Asistente con IA (carita animada):** abajo a la izquierda, una carita que sigue el cursor abre un chat. Responde con los datos reales del negocio (carta y precios de la base, terapeutas activas, turnos libres, horario, políticas de pago, cancelación y puntos) y también ejecuta órdenes en la página: "llévame a las cabinas", "muéstrame el nuru", "reservar 4 manos", "WhatsApp". Tiene micrófono y voz del navegador. No crea reservas: reservar exige cuenta de cliente y pago completo en `/reserva`.
- **Marquesina Cinética (*Kinetic Ticker*):** Cinta animada continua tipográfica con separadores estrellados (`✦`) para marcar el ritmo visual editorial.
- **Reloj en Vivo del Sanctum:** Indicador de atención activa con pulso luminoso (`SANCTUM OPEN • ATENCIÓN PRIVADA`) y reloj en tiempo real sincronizado con la zona horaria oficial de Lima, Perú (UTC-5).
- **Laboratorio de 5 Experiencias Awwwards (`/PRUEBAS/`):** Galería interactiva integrada con 5 estilos galardonados: *Kinetic Brutalist*, *Cyberpunk Jelly Glow 3D*, *Zen Haute Parfumerie*, *Liquid Spatial Motion* y *Avant-Garde Noir Dossier*.
- **Despacho Directo a WhatsApp:** Enlace automático hacia la central de atención (+51 924 040 992) con plantilla pre-formateada de cita, fecha, masajista y cabina.

### 2. Backend: API REST Segura & Alta Cohesión
- **Java 21 LTS + Spring Boot 3.3.5:** Arquitectura limpia con separación estricta en capas (Controllers, Services, Repositories, Entities, DTOs y Mappers).
- **Seguridad Sin Estado con JWT (Spring Security 6):** Filtro de interceptación de cabeceras `Authorization: Bearer <token>`, encriptación BCrypt para credenciales y tokens firmados con JJWT 0.12.6.
- **Control de Acceso Basado en Roles (RBAC):**
  - `ROLE_ADMIN`: Control total: catálogo de servicios, tarifas, cabinas, personal y clientes.
  - `ROLE_RECEPCIONISTA`: Gestión de reservas (crear, buscar, actualizar, cancelar) y confirmación de pagos.
  - `ROLE_ESPECIALISTA`: Consulta de su propia agenda futura (vinculado a su ficha de empleado).
  - `ROLE_CLIENTE`: Consulta de catálogo, disponibilidad de turnos, reserva a su nombre, pago completo y cancelación de sus propias reservas.
- **Cancelación sin borrado:** cancelar una reserva (`PATCH /api/db/reservas/{id}/cancelar`) solo cambia su estado a `CANCELADA`; se conserva el historial y se libera el horario. Un servicio, especialista o cabina con reservas activas (o con historial) no se puede eliminar (400).
- **Sin pérdida para el cliente:** si se cancela una sesión ya pagada (por el personal o por el cliente dentro de la política), el importe pagado queda como un **crédito** (código de un solo uso, sin costo en puntos) que se aplica al cobrar otra sesión; si el crédito supera el precio, el saldo queda como crédito nuevo. Un código de canje consumido por esa reserva vuelve a quedar disponible. No asistir (no-show) no genera crédito.
- **Persistencia Dual (Producción & Testing):** Soporte nativo para MySQL 8 mediante Docker Compose y perfiles en memoria H2 para pruebas unitarias automatizadas.
- **Documentación Viva OpenAPI 3 / Swagger UI:** Interfaz interactiva para pruebas directas de endpoints en `/swagger-ui/index.html`.
- **Validaciones Rigurosas:** Validación declarativa con `spring-boot-starter-validation` (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Positive`, `@Max`; la fecha futura de la reserva se valida en `ReservaService`) y manejador global de excepciones con respuestas estructuradas RFC 7807.

---

## 💆 Carta Oficial de Tratamientos & Tarifas (Soles S/)

Todas las experiencias están configuradas con tarifas oficiales en moneda nacional (Soles Peruanos - S/) y se llevan a cabo en cabinas climatizadas con iluminación tenue neón y privacidad absoluta:

| Código | Tratamiento Sensorial | Categoría | Duración | Tarifa (S/) | Técnica y Elementos Clave |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **RC-01** | **Masaje Tántrico Sensitivo** | TÁNTICO | 60 min | **S/ 200** | Toques lentos y continuos de pies a cabeza con aceites tibios neutros en atmósfera nocturna. |
| **RC-02** | **Masaje Nuru Body Slide** | NURU | 75 min | **S/ 280** | Cuerpo a cuerpo sobre camilla impermeable con gel nuru tibio hiperdeslizante y ducha privada. |
| **RC-03** | **Masaje Relajante & Descontracturante** | DESCONTRACTURANTE | 60 min | **S/ 180** | Presión media y descompresión cervical para liberar sobrecargas musculares en cuello y espalda. |
| **RC-04** | **Masaje Tántrico Completo** | TÁNTRICO | 75 min | **S/ 250** | Sesión tántrica integral con piedras tibias de cuarzo, aceites aromáticos y música binaural. |
| **RC-05** | **Masaje a Cuatro Manos** | 4 MANOS | 60 min | **S/ 380** | Dos terapeutas sincronizadas trabajando simultáneamente en maniobras espejo armónicas. |
| **RC-06** | **RECOVR VIP Hidromasaje** | VIP | 90 min | **S/ 450** | Experiencia completa: sensitivo tántrico, cuerpo a cuerpo Nuru y tina privada con microburbujas. |

### Staff de Terapeutas Acreditadas
- **Ely:** Especialista en Masaje Tántrico Sensitivo y aceites tibios neutros.
- **Miranda:** Experta en Masaje Nuru Body Slide y deslizamientos corporales sobre camilla atérmica.
- **Pamela:** Especialista en Descontracturante, puntos gatillo y sesiones dobles a Cuatro Manos.
- **Maria:** Atención exclusiva en Suite Presencial: masaje sensitivo, hidromasaje y microburbujas.

### Cabinas de Relax Climatizadas
- **Cabina 01 (Néon Rose):** Camilla térmica extra ancha, sábanas satinadas y cromoterapia neón rosa tenue.
- **Cabina 02 (Nuru Suite):** Colchón impermeable especial atérmico y ducha privada de alta presión en suite.
- **Cabina 03 (Dúo / 4 Manos):** Espacio amplio insonorizado para sesiones simultáneas en sincronía acústica.
- **Cabina 04 (VIP Hidromasaje):** Suite privada con tina de hidromasaje con microburbujas oxigenadas.

## 🔐 Seguridad y roles

- **Un solo acceso para todos los roles** (`/admin/login`): el navegador no guarda usuarios ni claves, solo el token JWT. La identidad y el rol se confirman con `GET /api/auth/me` y cada petición vuelve a validar el rol en el servidor.
- **Cada rol ve solo su zona:** CLIENTE → *Mi cuenta*; ESPECIALISTA → *Mi agenda* y *Fichas de clientes* (solo de clientes que atiende); RECEPCIONISTA → resumen, agenda, clientes y caja; ADMIN → todo lo operativo, personal, catálogo, sistema y auditoría. Las fichas confidenciales no las ve ni ADMIN ni RECEPCIONISTA.
- **Matriz de permisos verificada:** `GET /api/seguridad/matriz` lista qué rol puede cada acción y la prueba `SeguridadRolesTest` ejecuta cada fila con un token de cada rol.
- **Contraseñas:** BCrypt, mínimo 8 caracteres con letras y números. **Bloqueo:** 5 intentos fallidos de un mismo correo e IP bloquean 5 minutos (HTTP 429). El mensaje de error no revela si el correo existe.
- **Auditoría:** inicios de sesión, cambios de datos y accesos denegados quedan en la tabla `auditoria` (sin cuerpos ni contraseñas), visible para ADMIN en *Sistema y auditoría*.
- **Quién crea a quién:** los clientes se registran solos; **recepción, administración y terapeutas los crea un ADMIN** en *Usuarios y accesos* (clave temporal que se muestra una sola vez y cambio de contraseña obligatorio al primer ingreso; se puede desactivar o restablecer cualquier acceso). El **primer ADMIN** nace por configuración (`ADMIN_EMAIL` y `ADMIN_PASSWORD`), no por un endpoint público.
- **Puntos:** 1 punto por cada S/ 1 pagado en una sesión completada; saldo = ganados − canjeados; el nivel (Bronce, Plata, Oro, Diamante) depende de lo ganado. Se canjean por un código de un solo uso que recepción aplica al cobrar (el servidor recalcula el monto). Todo se calcula de reservas y canjes reales y se explica en *Mi cuenta*.
- **Perfil público de las terapeutas:** el sitio lee de la base de datos (solo activas): nombre, especialidad, presentación y la **foto que sube la propia terapeuta** (o el administrador) con su consentimiento. Las fotos se validan por firma (JPG/PNG, máx. 3 MB) y se re-codifican, lo que elimina metadatos.
- **Modo demo vs. modo real:** `./run-demo.sh` (`SEED_DEMO_DATA=true`) carga 11 cuentas y ~600 reservas de práctica; `./run-real.sh` arranca sin nada de eso (catálogo oficial + primer admin por configuración). `backend/bdd/limpiar-demo.sql` borra los datos de demostración de una base ya usada.
- **Reservas (reglas en `ReglasReserva` y `ReservaService`):**
  - **Solo el pago completo asegura el turno.** Una reserva nace `PENDIENTE` y retiene el cupo **15 min** desde que se crea; si no se paga el total del servicio en ese plazo, expira y el cupo se libera. El servidor rechaza cualquier monto distinto del precio (menos el código de canje, que decide el servidor) y el estado no se cambia a mano por `PUT` (confirmar exige el pago; completar exige el check-in).
  - **Contra el acaparamiento:** un cliente puede tener **1 reserva pendiente** a la vez, y quien deja vencer **3 reservas sin pagar en 24 h** queda bloqueado temporalmente (HTTP 429).
  - **Una persona no está en dos sitios:** un cliente no puede tener dos sesiones que se crucen, aunque sean con terapeutas y cabinas distintas. Dos clientes distintos sí pueden reservar a la misma hora con terapeuta y cabina distintas.
  - **Horario de atención 12:00–23:00:** la sesión completa debe caber en el horario (lo valida el servidor, no solo la pantalla). Una terapeuta con **ausencia** registrada o una **cabina en mantenimiento** no reciben reservas.
  - **Concurrencia:** crear, mover, cobrar y canjear bloquean la fila del cliente, de la terapeuta y de la cabina (bloqueo pesimista, en orden fijo, con aislamiento `READ_COMMITTED`). Dos peticiones simultáneas por el mismo recurso se procesan una tras otra y la segunda recibe `409`. Verificado con hilos en H2 y en MySQL 8.
  - **Cancelación del cliente:** con 4 h de anticipación y máximo 2 veces al mes. El negocio opera en hora de Lima (`America/Lima`).
  - **Capacidad de la cabina** es informativa (personas que caben); una cabina atiende una reserva a la vez.

---

## 🚀 Requisitos Previos

Asegúrate de contar con el siguiente entorno instalado en tu máquina local:
- **Java JDK:** Versión `21 LTS` ([Descargar OpenJDK 21](https://adoptium.net/))
- **Node.js:** Versión `>= 22.0.0` y npm `>= 10.0.0` ([Descargar Node.js](https://nodejs.org/))
- **Docker & Docker Compose:** ([Descargar Docker Desktop](https://www.docker.com/products/docker-desktop/))
- **Git:** Para control de versiones

---

## 🛠️ Guía de Instalación y Ejecución Local

### 1. Clonar el Repositorio
```bash
git clone https://github.com/ProgramaGrueso/RECOVR.git
cd RECOVR
```

### 2. Iniciar la Base de Datos (MySQL con Docker)
En una terminal, inicia el contenedor de base de datos MySQL 8 preconfigurado para RECOVR:
```bash
cd backend
docker compose up -d
cd ..
```
> **Nota:** La base de datos estará disponible en `127.0.0.1:3306` con la base `recovr_db`, usuario `root` y contraseña `root` (credenciales de demostración, ver `backend/docker-compose.yml`).

### 3. Iniciar el Backend (Spring Boot 3.3.5)
Requiere Java 21 y Maven 3.9+ (el repositorio no incluye el wrapper `mvnw`). Con la base de datos ya levantada:
```bash
cd backend
SEED_DEMO_DATA=true mvn spring-boot:run
```
*(En Windows PowerShell: `$env:SEED_DEMO_DATA="true"; mvn spring-boot:run`)*

- La API inicia en: `http://localhost:8095` (se cambia con la variable `SERVER_PORT`).
- Documentación Swagger UI interactiva: [http://localhost:8095/swagger-ui/index.html](http://localhost:8095/swagger-ui/index.html)
- Especificación OpenAPI en formato JSON: `http://localhost:8095/v3/api-docs`
- Variables opcionales: `JWT_SECRET` (clave Base64 de 32 bytes o más; si falta se genera una temporal al arrancar), `DB_USERNAME`, `DB_PASSWORD` y `SHOW_SQL=true`.
- CORS solo acepta orígenes conocidos: `localhost` / `127.0.0.1` (cualquier puerto) y dominios `https://*.vercel.app`. Un origen cualquiera no recibe la cabecera `Access-Control-Allow-Origin`.

**Datos de demostración (`SEED_DEMO_DATA=true`):** al arrancar por primera vez carga el catálogo oficial (RC-01 a RC-06), las 4 cabinas, las 4 terapeutas, 45 clientes y unas 600 reservas entre el 1 de octubre y el 31 de diciembre de 2026 (rango configurable con `recovr.seed.inicio` y `recovr.seed.fin`). Respeta la regla de negocio: ninguna cabina ni terapeuta tiene dos reservas activas que se crucen (duración + tiempo de limpieza). Las sesiones pasadas quedan COMPLETADAS o CANCELADAS y las futuras CONFIRMADAS (con pago) o PENDIENTES. Es idempotente: si ya hay datos demo, no vuelve a cargarlos. Con `DEMO_PASSWORD` crea además las cuentas `admin.demo@recovr.test`, `recepcion.demo@recovr.test`, `ely.demo@recovr.test` (ESPECIALISTA) y `cliente.demo@recovr.test`.

#### Atajo para el equipo: base de datos + backend con datos de 3 meses (oct–dic)
Con Docker, Java 21 y Maven instalados, en la carpeta `backend`:
```bash
./run-demo.sh          # Linux / macOS
.\run-demo.ps1         # Windows PowerShell
```
El script levanta MySQL (`docker compose up -d`), espera a que esté sano y arranca el backend con `SEED_DEMO_DATA=true`. Con `SEED_DEMO_DATA=true` se crean 11 cuentas de demostración (administración, recepción, las 4 terapeutas y 5 clientes), cada una con su propia contraseña: están listadas en `backend/CREDENCIALES_DEMO.md` (archivo local, no se sube a git). Solo para desarrollo.
> Si Docker responde que el nombre `recovr-mysql` ya está en uso, es un contenedor de una ejecución anterior: `docker start recovr-mysql` (o `docker rm recovr-mysql` y repetir).

#### Asistente con IA (opcional)
El chat de la portada usa DeepSeek desde el backend (`POST /api/ia/chat`, público y con límites por IP). La clave solo vive en el servidor:
```bash
DEEPSEEK_API_KEY=sk-... SEED_DEMO_DATA=true mvn spring-boot:run
```
Sin `DEEPSEEK_API_KEY` el chat responde que no está disponible y ofrece WhatsApp; las órdenes de navegación de la carita siguen funcionando. `IA_REGISTRAR_CONVERSACIONES=true` deja en el log lo que se pregunta (solo para demos). Límites y datos del negocio en `application.properties` (`recovr.ia.*`, `recovr.negocio.*`).

### 4. Iniciar el Frontend (Angular Standalone)
En una nueva terminal, instala las dependencias y corre el servidor de desarrollo:
```bash
cd frontend
npm install
npm start
```
- La aplicación principal estará disponible en: [http://localhost:4200](http://localhost:4200)
- **Integración con el backend:** el flujo de reserva (`/reserva`) inicia sesión o registra al cliente en la API y crea reservas reales en MySQL; la cabina la asigna el servidor. La URL de la API (por defecto `http://localhost:8095/api`) se cambia con `localStorage['recovr-api-url']`. Si el backend no responde, el flujo usa una simulación local para que la demo publicada en Vercel siga funcionando. El resto del panel administrativo (`DEMO_USERS`) sigue siendo simulado en el navegador.
- Acceso directo al **Lab de Experiencias Awwwards**: [http://localhost:4200/PRUEBAS/](http://localhost:4200/PRUEBAS/)
- Panel de inicio de sesión administrativo: [http://localhost:4200/admin/login](http://localhost:4200/admin/login)

### 5. Iniciar el Prototipo Independiente (Vite Lab Opcional)
Si deseas explorar el entorno de prototipado rápido con recarga ultra-veloz HMR en Vite:
```bash
cd Prototipo
npm install
npm run dev
```
- Prototipo activo en: `http://localhost:5173`

---

## 🔐 Matriz de Seguridad y Endpoints REST

| Método | Endpoint | Rol requerido | Descripción de la operación |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/registro`, `/api/auth/login` | *Público* | Registro de clientes, alta del primer ADMIN e inicio de sesión (emiten JWT). |
| `POST` | `/api/auth/usuarios` | `ADMIN` | Alta de RECEPCIONISTA o ESPECIALISTA (opcionalmente vinculado a un empleado con `empleadoId`). |
| `GET` | `/api/servicios`, `/api/servicios/{id}` | Cualquier rol autenticado | Catálogo de terapias con duración, tiempo de limpieza y precio. |
| `POST` `PUT` `DELETE` | `/api/servicios/**` | `ADMIN` | CRUD del catálogo (201 al crear, 204 al eliminar). |
| `GET` | `/api/empleados/catalogo` | Cualquier rol autenticado | Terapeutas (id, nombre, especialidad) para armar una reserva, sin datos de contacto. |
| `*` | `/api/clientes/**`, `/api/empleados/**`, `/api/salas/**`, `/api/pagos/**` | `ADMIN` | CRUD de administración; `GET /api/salas/disponibles` y `/api/empleados/disponibles` consideran duración + limpieza. |
| `POST` | `/api/db/reservas` | `CLIENTE`, `RECEPCIONISTA`, `ADMIN` | Crea una reserva PENDIENTE que retiene el cupo 15 min (si no se indica `salaId`, asigna la primera cabina libre); responde 409 si la sala, la terapeuta o el propio cliente ya tienen una sesión que se cruza, o si ya tiene una pendiente de pago; 400 fuera del horario 12:00–23:00; 429 si dejó vencer demasiadas reservas sin pagar. |
| `GET` | `/api/db/reservas/disponibilidad?empleadoId&servicioId&fecha` | `CLIENTE`, `RECEPCIONISTA`, `ADMIN` | Turnos del día con su disponibilidad real y el motivo cuando no se puede (terapeuta ocupada, sin cabinas, fuera de horario, ausencia, cliente con otra sesión). |
| `GET` | `/api/db/reservas/mias` | `CLIENTE` | Historial del cliente autenticado. |
| `GET` | `/api/db/reservas/agenda` | `ESPECIALISTA` | Agenda futura del especialista autenticado. |
| `GET` | `/api/db/reservas/cliente/{id}` | `CLIENTE` (solo las suyas), `RECEPCIONISTA`, `ADMIN` | Historial de un cliente. |
| `POST` | `/api/db/reservas/{id}/confirmar-y-pagar` | `CLIENTE` (solo las suyas), `RECEPCIONISTA`, `ADMIN` | Confirma la reserva y registra el pago en una transacción. El monto debe ser el total del servicio (400 si no); fuera del plazo de 15 min se rechaza. |
| `PATCH` | `/api/db/reservas/{id}/cancelar` | `CLIENTE` (solo las suyas), `RECEPCIONISTA`, `ADMIN` | Cancela una reserva PENDIENTE o CONFIRMADA cambiando su estado a CANCELADA (no borra la fila); si ya estaba pagada, el importe queda como crédito del cliente. |
| `GET` `PUT` | `/api/db/reservas`, `/api/db/reservas/{id}`, `/api/db/reservas/buscar` | `RECEPCIONISTA`, `ADMIN` | Listado, reprogramación (misma validación de agenda; el estado no se cambia a mano salvo cancelar) y búsqueda JPQL por rango de fechas y estado. |
| `DELETE` | `/api/db/reservas/{id}` | `ADMIN` | Elimina solo reservas CANCELADAS (las demás son historial). |
| `PATCH` | `/api/salas/{id}/activa?valor=` | `ADMIN` | Pone una cabina en mantenimiento o la reactiva (400 si tiene reservas activas). |
| `GET` `POST` `DELETE` | `/api/empleados/{id}/ausencias`, `/api/empleados/ausencias/{id}` | `ADMIN` | Vacaciones o licencias de una terapeuta: mientras duran no recibe reservas. |

Códigos de error: `400` datos inválidos (con detalle por campo), `401` sin token o token inválido, `403` rol sin permiso, `404` recurso inexistente (o intento de eliminar un servicio, especialista o cabina con reservas: `400`), `409` cruce de horario, reserva pendiente sin pagar o registro duplicado, `429` demasiados intentos de acceso o demasiadas reservas vencidas sin pagar.

---

## 🧪 Estrategia de Pruebas y Control de Calidad

### Pruebas Unitarias del Backend (TDD)
Ejecuta la suite de pruebas unitarias con simulación de capas de persistencia y servicios:
```bash
cd backend
mvn test
```
- Pruebas de controladores REST con `MockMvc`.
- Validaciones de negocio en `ReservaService` y `ClienteService`.
- Pruebas de integración con perfiles de base de datos H2 en memoria (JWT real, roles, conflictos de agenda, asignación de cabina, cancelación sin borrado, borrado protegido y seed de datos demo).
- `ReglasNegocioTest`: reglas de agenda, cobro completo, plazo de pago, bloqueo por reservas vencidas, horario, crédito por cancelación, cabinas en mantenimiento, ausencias y disponibilidad.
- `ConcurrenciaReservaTest`: dos hilos con transacciones reales compiten por la misma terapeuta, cabina, cliente, cobro o saldo de puntos; solo uno gana. Sin los bloqueos, el mismo test crea doble reserva. Para repetirlo sobre MySQL 8, sobrescribe `spring.datasource.*` con `-D` (ver `docker-compose.yml`).

### Pruebas del Frontend
```bash
cd frontend
npm test
```
Cubren `ApiService` (login, creación de reserva y error 409), `BookingService` (sin sesión o sin backend informa el error: nunca simula una reserva), disponibilidad de turnos y pago completo.

### Compilación y Verificación de Presupuestos de Frontend
Verifica que el bundle de producción no supere los presupuestos estrictos de rendimiento:
```bash
cd frontend
npm run build
```
- **Presupuesto inicial estricto:** `< 500 kB` (cumplido exitosamente en ~395 kB).
- **Carga diferida:** Módulos de administración, catálogo y Three.js divididos en chunks independientes (*lazy loading*).

---

## ☁️ Despliegue e Integración Continua (CI/CD)

- **GitHub Actions:** Cada `push` o `pull request` hacia la rama `main` ejecuta un flujo de verificación con Node.js 22 y Java 21, validando la compilación del frontend y las pruebas unitarias del backend.
- **Vercel Monorepo:** Configurado en `vercel.json` para compilar y distribuir de forma óptima el build del frontend (`frontend/dist/recovr-frontend/browser`) con compresión gzip/brotli y soporte SPA con reescritura de rutas (`rewrites`).

---

## 🎓 Contexto Académico

- **Institución:** Universidad Tecnológica del Perú (UTP)
- **Curso:** Desarrollo Web Integrado
- **Organización / Equipo:** ProgramaGrueso
- **Sustentaciones:** Expedientes completos organizados en el directorio [`exposicion/`](./exposicion) (APF1, APF2, APF3, mapas de APIs, diapositivas de sustentación y guías por integrante).

---

## 📄 Licencia

Este proyecto ha sido desarrollado con fines académicos y de demostración tecnológica de alto nivel. Todos los derechos reservados &copy; 2026 RECOVR Sanctum.

---

> **Nota sobre credenciales:** los usuarios de `frontend/src/app/services/auth.service.ts` (`DEMO_USERS`) y las credenciales de `docker-compose.yml` son **datos de demostración** para la exposición académica. No corresponden a ningún sistema real.
