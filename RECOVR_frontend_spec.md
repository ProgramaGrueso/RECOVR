# RECOVR — Especificación de Frontend

Spa de recuperación masculina (masaje terapéutico/deportivo + podología clínica).
Identidad visual: editorial oscuro, gótico-premium, inspirado en el lenguaje visual de Serotoninn.

---

## 1. Identidad de marca

- **Nombre**: RECOVR
- **Categoría**: recuperación física masculina — masaje + podología
- **Tono**: clínico-pero-sensual, gótico-editorial, nunca "spa pastel genérico"
- **Tagline (borrador)**: *"El cuerpo, restaurado."* — ajustar en fase de copy

---

## 2. Design tokens

### Color

| Token | Hex (aprox.) | Uso |
|---|---|---|
| `--color-bg` | `#0A0A0B` | Negro profundo, fondo base |
| `--color-bg-alt` | `#141014` | Negro con matiz para secciones alternadas |
| `--color-accent` | `#5C1A2B` | Borgoña oscuro — acento principal (CTAs, líneas, hover) |
| `--color-accent-soft` | `#7A2E3F` | Variante más clara del acento, usar con moderación |
| `--color-text` | `#F2EDEA` | Blanco roto, nunca blanco puro |
| `--color-text-muted` | `#8A8285` | Texto secundario, labels, captions |
| `--color-metal` | `#B8B4B0` | Detalles plateados (líneas finas, íconos) |

### Tipografía

| Rol | Fuente propuesta | Uso |
|---|---|---|
| Display / Wordmark | Bodoni Moda o Cormorant, mayúsculas, tracking amplio | H1, nombre de marca, títulos de sección |
| Body | Sans neutra (ej. Inter, Neue Montreal) | Párrafos, descripciones de servicio |
| Data/UI | Misma sans, weight regular/medium | Precios, duración, horarios, botones |
| Símbolo decorativo | Solo el ícono generado (cruz gótica), nunca texto completo en blackletter | Favicon, marca de agua, separadores de sección |

> Nota: el wordmark "RECOVR" se implementa en tipografía real vía CSS/Google Fonts — **no** como imagen generada por IA. El símbolo gótico (Imagen 2 / asset `symbol-cross`) se usa como ícono acompañante, no como logotipo de texto.

---

## 3. Inventario de assets generados

| Asset | Archivo de referencia | Uso en el sitio |
|---|---|---|
| **Hero video** | `Video_00025.mp4` — retrato editorial con push-in de cámara | Fondo del hero principal, loop, con wordmark superpuesto en el espacio negativo (lado izquierdo del encuadre) |
| **Producto/detalle** | `Krea2-155101` (frascos, envoltorio negro, piedras) | Sección de "protocolos"/servicios, tarjetas de producto, íconos de categoría |
| **Ambiente/sala** | `Krea2-155013` (sala de tratamiento gótica: candelabro, espejo, camilla) | Sección "quiénes somos" / "el espacio", banner de sección de reservas |
| **Símbolo de marca** | Imagen de la cruz gótica ornamental | Favicon, loader/spinner, separador entre secciones, marca de agua sutil en fondos |
| *(Pendiente)* Detalle de textura | Regenerar con prompt ajustado (vendajes, aceites, piedras — sin elementos corporales) | Micro-detalles decorativos entre bloques de contenido |
| *(Pendiente)* Retratos de kinesiólogas | Uno por profesional, mismo estilo que el hero | Sección "equipo" |

---

## 4. Estructura de páginas / secciones

### 4.1 Home

1. **Hero** — video de fondo en loop + wordmark + tagline + CTA "Reservar sesión"
2. **Categorías** — `MASAJE [4]` / `PODOLOGÍA [4]`, estilo corchetes tipo Serotoninn
3. **Protocolos destacados** — grid de paquetes/combos (`[ PROTOCOLO 01 ]`, `[ PROTOCOLO 02 ]`...) usando asset de producto
4. **El espacio** — imagen de ambiente + copy corto sobre la experiencia
5. **Equipo** — grid de kinesiólogas/podólogos, foto editorial + especialidad
6. **CTA final + footer**

### 4.2 Catálogo de servicios
- Filtro por categoría (Masaje / Podología)
- Cada servicio: nombre, duración, precio, profesional(es) habilitados

### 4.3 Flujo de reserva
- Selección de servicio → selección de profesional (filtrado por especialidad) → selección de sala/horario disponible → confirmación
- Estado de reserva visible en cuenta de usuario: pendiente / confirmada / completada / cancelada

### 4.4 Panel admin
- Calendario de ocupación por profesional/sala
- Reportes básicos (ingresos, servicios más reservados, tasa de cancelación)
- Gestión de usuarios, servicios, paquetes

---

## 5. Componentes UI clave (Angular)

- `HeroComponent` — video de fondo + overlay de texto
- `ServiceCardComponent` — tarjeta reutilizable (imagen, nombre, duración, precio, CTA)
- `ProfessionalCardComponent` — foto + nombre + especialidad + disponibilidad
- `BookingFlowComponent` — stepper de reserva (servicio → profesional → horario → confirmación)
- `SectionDividerComponent` — usa el símbolo de marca como separador visual entre bloques

---

## 6. Notas de implementación (SASS)

```scss
// _tokens.scss
:root {
  --color-bg: #0A0A0B;
  --color-bg-alt: #141014;
  --color-accent: #5C1A2B;
  --color-accent-soft: #7A2E3F;
  --color-text: #F2EDEA;
  --color-text-muted: #8A8285;
  --color-metal: #B8B4B0;

  --font-display: 'Bodoni Moda', serif;
  --font-body: 'Inter', sans-serif;
}
```

- Comprimir el hero video a WebM/H.265 para producción (el MP4 original es liviano, pero conviene optimizar antes de deploy)
- Respetar `prefers-reduced-motion` para el push-in de cámara y cualquier animación de scroll
- Contraste de texto sobre video: usar overlay `rgba(10,10,11,0.35)` si el texto pierde legibilidad sobre frames más claros del hero

---

## 7. Voz y copy

- Frases cortas, directas, tono ritual/ceremonial (coherente con `[ PROTOCOLO 01 ]`)
- Nunca vocabulario clínico frío puro, ni tampoco lenguaje "wellness" genérico (evitar "mindfulness", "self-care", clichés de spa)
- CTAs en voz activa: "Reservar sesión", no "Haz tu reserva aquí"
