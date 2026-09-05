# RECOVR — Estructura de Roles y Guía para la Exposición

Este documento define los roles del equipo, el orden de intervención y la distribución de temas para la defensa/exposición del proyecto **RECOVR (Spa de Recuperación Masculina - Dashboard & UI)**.

---

## 1. Roles del Proyecto y Exposición (Equipo de 4-5 personas)

### Rol 1: Presentador / Product Owner & Visión de Negocio
* **Enfoque:** Introducción, propuesta de valor e identidad.
* **Responsabilidades en la exposición:**
  - Presentar la introducción del proyecto: ¿Qué es **RECOVR**?
  - Explicar la oportunidad de mercado: spa de recuperación física masculina (kinesiología, masajes deportivos/terapéuticos y podología clínica).
  - Justificar el concepto de marca: tono editorial oscuro y gótico-premium (lejos del spa pastel convencional).
  - Explicar el problema que resuelve la plataforma web frente a las reservas tradicionales.
* **Tiempo sugerido:** 2 - 3 minutos.

---

### Rol 2: Diseñador UI/UX & Frontend Styling
* **Enfoque:** Identidad visual, Design Tokens y experiencia de usuario.
* **Responsabilidades en la exposición:**
  - Explicar la guía de estilos y *Design Tokens*:
    - Paleta de color: negro profundo (`#0A0A0B`), borgoña acento (`#5C1A2B`), detalles plateados (`#B8B4B0`).
    - Tipografía editorial: *Bodoni Moda / Cormorant* (Display) y *Inter / Neue Montreal* (cuerpo y datos).
  - Mostrar la integración de assets multimedia y componentes visuales:
    - Hero con video de fondo en loop y overlay.
    - Símbolo de marca (cruz gótica ornamental) como separador (`SectionDividerComponent`).
    - Cursor personalizado (`CustomCursorComponent`).
  - Justificar la navegación fluida y la experiencia del usuario (UX).
* **Tiempo sugerido:** 2 - 3 minutos.

---

### Rol 3: Desarrollador Frontend Angular (Core & Flujo de Cliente)
* **Enfoque:** Arquitectura técnica, catálogo y flujo de reserva.
* **Responsabilidades en la exposición:**
  - Detallar el stack tecnológico: **Angular**, TypeScript, SCSS modular y arquitectura de componentes (*Standalone Components*).
  - Explicar la organización de rutas (`app.routes.ts`) y servicios de datos (`catalog.service.ts`, `booking.service.ts`).
  - Demostrar el catálogo interactivo de servicios y protocolos:
    - Filtros por categoría (`MASAJE`, `PODOLOGÍA`).
    - `ServiceCardComponent` y tarjetas de especialistas (`ProfessionalCardComponent`).
  - Demostrar el flujo interactivo de reserva (*Booking Flow* por pasos):
    - Selección de servicio $\rightarrow$ profesional $\rightarrow$ fecha y hora $\rightarrow$ confirmación.
* **Tiempo sugerido:** 3 - 4 minutos.

---

### Rol 4: Desarrollador Fullstack / Dashboard Admin
* **Enfoque:** Panel de administración, métricas y gestión de datos.
* **Responsabilidades en la exposición:**
  - Presentar la vista de administración (`/admin`):
    - Resumen de métricas clave (ingresos, total de citas, tasa de ocupación).
    - Calendario de ocupación por profesional y sala.
    - Lista y gestión de estados de reservas (pendiente, confirmada, cancelada).
  - Explicar la lógica de negocio detrás del dashboard y el modelado de datos (`models/`).
  - Describir cómo se sincronizan las reservas entre el cliente y el panel de administración.
* **Tiempo sugerido:** 3 minutos.

---

### Rol 5: QA, DevOps & Conclusiones (Control de Calidad y Cierre)
* **Enfoque:** Flujo de desarrollo, calidad de software, demo y cierre.
* **Responsabilidades en la exposición:**
  - Explicar el flujo de trabajo en Git/GitHub (manejo de ramas, ej. `feature-02-dashboard-ui`).
  - Resumir las buenas prácticas implementadas (responsividad, modularidad, rendimiento).
  - Coordinar o asistir en la **Demo en Vivo** (Live Demo).
  - Presentar conclusiones, lecciones aprendidas y trabajo futuro (ej. pasarela de pagos, autenticación con JWT, notificaciones SMS/WhatsApp).
  - Gestionar la ronda de preguntas y respuestas (Q&A).
* **Tiempo sugerido:** 2 - 3 minutos.

---

## 2. Adaptación según el tamaño del equipo

| Integrantes | Distribución de Roles |
|---|---|
| **2 Personas** | **Persona A:** Negocio, UI/UX y Conclusiones.<br>**Persona B:** Arquitectura Angular, Flujo de Reserva y Dashboard Admin. |
| **3 Personas** | **Persona 1:** Negocio + UI/UX y Diseño.<br>**Persona 2:** Arquitectura Angular + Catálogo y Reserva.<br>**Persona 3:** Dashboard Admin + DevOps, QA y Conclusiones. |
| **4 Personas** | **Persona 1:** Negocio y Presentación general.<br>**Persona 2:** UI/UX, Estilos y Componentes visuales.<br>**Persona 3:** Flujo de Reserva y Servicios Angular.<br>**Persona 4:** Dashboard Admin, Pruebas y Cierre. |
| **5 Personas** | Aplicar el esquema completo de los 5 roles detallados en la Sección 1. |

---

## 3. Estructura recomendada de la presentación (10 a 15 minutos)

1. **Introducción y Contexto (2 min):** Portada, integrantes, problema y concepto RECOVR.
2. **Diseño y Experiencia (2 min):** Paleta oscura, tipografía editorial y diseño centrado en el usuario masculino.
3. **Demostración en Vivo - Vista Cliente (4 min):** Navegación Home, catálogo, servicios y reserva paso a paso.
4. **Demostración en Vivo - Vista Admin (3 min):** Dashboard de gestión, métricas y control de citas.
5. **Arquitectura Técnica y Código (2 min):** Estructura del proyecto Angular, servicios y modelos.
6. **Conclusiones y Preguntas (2 min):** Retos superados, mejoras futuras y cierre.
