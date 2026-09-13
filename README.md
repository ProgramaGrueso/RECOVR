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
- **Cursor Magnético Reactivo (*Spring Physics*):** Cursor elástico con interpolación lineal (*lerp* continuo) que proyecta un anillo exterior translúcido (*glassmorphic ring*), resplandor neón y etiquetas contextuales automáticas mediante delegación de eventos en atributos `[data-cursor]`.
- **Efecto de Inclinación en Perspectiva 3D (`[appCardTilt]`):** Directiva Angular optimizada que calcula dinámicamente la posición del puntero y aplica transformaciones tridimensionales (`perspective`, `rotateX`, `rotateY`) en tiempo real ejecutadas 100% fuera de los ciclos de `NgZone`.
- **Atmósfera Somatosensorial con Three.js:** Lienzo WebGL con generación procedural de volutas de humo suave, luces de color neón rosa/púrpura/cian y ascuas luminosas ascendentes (*stardust*), optimizado con carga diferida `@defer (on viewport)` e `IntersectionObserver` para consumo 0% de GPU cuando no está en pantalla.
- **Reproducción de Video con Audio Inteligente en Hover:** Previsualizaciones de video en tarjetas de masajes y terapeutas con protocolo de evasión de restricciones de *Autoplay Policy* de los navegadores (fallback automático a silencioso sin arrojar excepciones).
- **Filtrado Dinámico de la Carta de Masajes:** Barra interactiva de píldoras neón para filtrar instantáneamente por categorías (`TODOS`, `TÁNTRICO`, `NURU BODY SLIDE`, `DESCONTRACTURANTE`, `4 MANOS`, `VIP HIDROMASAJE`).
- **Marquesina Cinética (*Kinetic Ticker*):** Cinta animada continua tipográfica con separadores estrellados (`✦`) para marcar el ritmo visual editorial.
- **Reloj en Vivo del Sanctum:** Indicador de atención activa con pulso luminoso (`SANCTUM OPEN • ATENCIÓN PRIVADA`) y reloj en tiempo real sincronizado con la zona horaria oficial de Lima, Perú (UTC-5).
- **Laboratorio de 5 Experiencias Awwwards (`/PRUEBAS/`):** Galería interactiva integrada con 5 estilos galardonados: *Kinetic Brutalist*, *Cyberpunk Jelly Glow 3D*, *Zen Haute Parfumerie*, *Liquid Spatial Motion* y *Avant-Garde Noir Dossier*.
- **Despacho Directo a WhatsApp:** Enlace automático hacia la central de atención (+51 924 040 992) con plantilla pre-formateada de cita, fecha, masajista y cabina.

### 2. Backend: API REST Segura & Alta Cohesión
- **Java 21 LTS + Spring Boot 3.3.5:** Arquitectura limpia con separación estricta en capas (Controllers, Services, Repositories, Entities, DTOs y Mappers).
- **Seguridad Sin Estado con JWT (Spring Security 6):** Filtro de interceptación de cabeceras `Authorization: Bearer <token>`, encriptación BCrypt para credenciales y tokens firmados con JJWT 0.12.6.
- **Control de Acceso Basado en Roles (RBAC):**
  - `ROLE_ADMIN`: Control absoluto de métricas, configuración de servicios, tarifas y asignación de personal.
  - `ROLE_RECEPCION`: Registro de citas presenciales, confirmación de pagos y control de agenda.
  - `ROLE_STAFF`: Acceso a turnos, historial de sesiones y estado de cabinas asignadas.
  - `ROLE_CLIENTE`: Consulta de catálogo, historial de reservas y puntos de fidelidad.
- **Persistencia Dual (Producción & Testing):** Soporte nativo para MySQL 8 mediante Docker Compose y perfiles en memoria H2 para pruebas unitarias automatizadas.
- **Documentación Viva OpenAPI 3 / Swagger UI:** Interfaz interactiva para pruebas directas de endpoints en `/swagger-ui/index.html`.
- **Validaciones Rigurosas:** Validación declarativa con `spring-boot-starter-validation` (`@NotNull`, `@Size`, `@Pattern`, `@FutureOrPresent`) y manejador global de excepciones con respuestas estructuradas RFC 7807.

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
> **Nota:** La base de datos estará disponible en el puerto `3306` con la base `recovr_db`, usuario `recovr_user` y contraseña `recovr_pass`.

### 3. Iniciar el Backend (Spring Boot 3.3.5)
En la carpeta `backend/`, ejecuta la aplicación mediante el wrapper de Maven:
```bash
cd backend
./mvnw spring-boot:run
```
*(En Windows PowerShell: `.\mvnw.cmd spring-boot:run`)*

- La API iniciará en: `http://localhost:8080`
- Documentación Swagger UI interactiva: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Especificación OpenAPI en formato JSON: `http://localhost:8080/v3/api-docs`

### 4. Iniciar el Frontend (Angular Standalone)
En una nueva terminal, instala las dependencias y corre el servidor de desarrollo:
```bash
cd frontend
npm install
npm start
```
- La aplicación principal estará disponible en: [http://localhost:4200](http://localhost:4200)
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

| Método | Endpoint | Rol Mínimo Requerido | Descripción de la Operación |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | *Público* | Autenticación con usuario/clave; emite token JWT con tiempo de expiración. |
| `POST` | `/api/auth/registro` | *Público* | Registro de nuevos clientes con rol `ROLE_CLIENTE`. |
| `GET` | `/api/servicios` | *Público* | Consulta del catálogo completo de masajes y tarifas en Soles. |
| `GET` | `/api/servicios/{id}` | *Público* | Detalle, duración y características de una sesión específica. |
| `POST` | `/api/reservas` | `ROLE_CLIENTE` | Creación de reserva con validación de disponibilidad horaria y cabina. |
| `GET` | `/api/reservas/mis-citas`| `ROLE_CLIENTE` | Historial y estado de las citas solicitadas por el cliente logueado. |
| `GET` | `/api/reservas` | `ROLE_RECEPCION` | Lista global de reservas con filtros por fecha, estado y terapeuta. |
| `PATCH`| `/api/reservas/{id}/estado`| `ROLE_RECEPCION` | Transición de estado (PENDIENTE → CONFIRMADA → EN_CURSO → FINALIZADA). |
| `GET` | `/api/profesionales` | `ROLE_CLIENTE` | Roster de terapeutas disponibles, especialidad y estado de turnos. |
| `POST` | `/api/admin/servicios` | `ROLE_ADMIN` | Alta o modificación de tratamientos, tarifas y recursos técnicos. |
| `GET` | `/api/admin/metricas` | `ROLE_ADMIN` | Métricas financieras (ingresos en S/), índice de ocupación y fidelización. |

---

## 🧪 Estrategia de Pruebas y Control de Calidad

### Pruebas Unitarias del Backend (TDD)
Ejecuta la suite de pruebas unitarias con simulación de capas de persistencia y servicios:
```bash
cd backend
./mvnw test
```
- Pruebas de controladores REST con `MockMvc`.
- Validaciones de negocio en `ReservaService` y `ClienteService`.
- Pruebas de integración con perfiles de base de datos H2 en memoria.

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
