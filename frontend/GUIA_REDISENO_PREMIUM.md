---
name: premium-web-3d
description: Rediseñar un frontend web a nivel "premium/Awwwards" con 3D (Three.js), scroll narrativo (GSAP+Lenis), assets generados con ComfyUI (Krea2 + MiniMax H3 y LoRAs de personajes) y optimización móvil verificada. Úsala cuando pidan una web "de vanguardia", "con efectos 3D", un rediseño visual completo o assets de imagen/video para una web. Probada en RECOVR (Angular 22), 2026-10-09.
---

# Rediseño web premium con 3D + assets IA

Receta validada en RECOVR (rama `redesign/awwwards-3d`, ~1 jornada). Código de referencia en `reference/`, scripts en `scripts/`. Aprendizajes en español; prompts de modelos en inglés.

## 0. Reglas de trabajo (el usuario las exigió)
1. **Rama aparte** (`redesign/...`). Nada a `main` hasta que el usuario diga que le gusta. Después: commit solo de tus archivos (el árbol suele traer cambios ajenos: videos borrados, docker-compose, xlsx → NO los metas) y merge; el `push` (despliega en Vercel) se confirma aparte.
2. **Dar el link del dev server** (`ng serve --port 4200`, el usuario lo abre en su misma máquina) y mostrar avances por fases; esperar OK antes de seguir con lo grande.
3. **Contenido**: sitios de masajes/spa → todo vestido, cliente cubierto con toalla y sin rostro, sin desnudos ni poses explícitas. "Sensual" = coqueta/juguetona, no explícita.
4. **No nombres personas reales en prompts de imagen** (ver §3). Si una cara se parece a una celebridad, descartar y regenerar.
5. Revisar visualmente cada render (montage de fotogramas) antes de ponerlo en el sitio.

## 1. Flujo por fases
1. Explorar el repo (componentes de efectos ya existentes, tokens de diseño, rutas, `README`). Reutilizar, no reescribir.
2. `npm i gsap lenis` (three suele estar). Servicio de scroll suave: `reference/smooth-scroll.service.ts` (Lenis, apagado con `prefers-reduced-motion`, fuera de NgZone).
3. **Hero 3D**: `reference/hero-orb.component.ts` — icosaedro con ruido simplex en vértice (normales por diferencias finitas), paleta de marca en fragment shader, burbujas satélite instanciadas, reacciona a mouse + scroll, pausa fuera de pantalla (IntersectionObserver), `prefers-reduced-motion` = un frame, sin WebGL = no rompe.
   - Fondo **oscuro**: `mix-blend-mode: screen` + `UnrealBloomPass` (sobreexpone fácil: bajar intensidades, `pow(col,1.35)`, bloom 0.4/0.7/0.55).
   - Fondo **claro/pastel**: sin bloom ni blend; `alpha:true`, `premultipliedAlpha:true`, salida `vec4(col*alpha, alpha)` con `alpha = .30 + fresnel*.75` → burbuja de jabón translúcida.
4. **Scroll narrativo anclado**: `reference/scroll-showcase.component.ts` (GSAP ScrollTrigger, `pin`, `scrub`, timeline: líneas con máscara, retratos con `clip-path`, parallax, cambio de escena, plano final a pantalla completa). Título DETRÁS de los retratos (z-index) da profundidad. En `max-width:768px` sin pin: apilado con reveal simple. Estático con `reduced-motion`.
5. **Paleta**: cambiar tokens (`_tokens.scss`) y fondo global primero; luego el shader de fondo. Ver §4 para el remapeo masivo de colores.
6. **Assets IA** (§3): retratos, escenas, tratamientos, texturas, videos H3.
7. **Optimización + auditoría responsive** (§5).

## 2. Entorno ComfyUI (máquina de juang)
- Servidor: `http://127.0.0.1:8188` (suele estar ya activo; si no: ver `/mnt/Datos/VideoMusical/PIPELINES.md`). Python de scripts: `~/ComfyUI/venv/bin/python`. **Un render a la vez** (RTX 4070 Ti SUPER 16 GB).
- Leer primero `/mnt/Datos/VideoMusical/CLAUDE.md` → `PIPELINES.md` → `H3_PIPELINE_PLAYBOOK.md`. Librerías: `shared/lib/{comfy_lib,krea2_lib,h3_lib}.py` (importar con `sys.path.insert`).
- `scripts/comfy_assets_template.py`: cola completa (keyframe → imágenes → video H3). Detalles que ya fallaron:
  - `comfy.run(wf)` devuelve **lista de rutas**, no el historial: usar `h if isinstance(h, list) else comfy.outputs(h)`.
  - `comfy.configure("<carpeta>")` antes de `comfy.stage()`; `comfy.TAG` = prefijo.
  - Lanzar con `nohup ... python -u script.py > log &` y esperar con bucle `until` sobre el log (un `sleep` largo en primer plano bloquea).
  - **No uses `pkill -f gen.py`**: coincide con tu propio comando y mata la shell. Matar por PID leyendo `/proc/*/cmdline`. Para cancelar: matar el script, `POST /interrupt` y `POST /queue {"clear":true}`.
- Krea2 t2i: `k2.t2i_wf(prompt, seed, "carpeta/nombre", lora=..., size=(w,h))` — `size` es la **1ª pasada**, la salida es ×2 (768×432 → 1536×864). ~40 s por imagen con modelos calientes.
- H3 (video): `h3.build_wf(h3.GANCHO + prompt, [keyframe_staged], length=168, seed, prefix, size=(960,544))` → 7,3 s a 24 fps, ~3-5 min. 16:9 usar (960,544); vertical (704,864). `h3.render(wf)` espera cola vacía. RefMod solo si hay UN personaje en cuadro (con cliente en escena se omitió y la identidad aguantó con el keyframe).
- Prompts H3 que funcionaron: escena fija + una acción corta + "looks up and gives a soft smile to the camera" + "camera slowly pushes in" + "Everyone stays fully clothed or covered".

### LoRAs de personajes (Krea2, `~/ComfyUI/models/loras`) y sus palabras
| Personaje del sitio RECOVR | LoRA | Trigger |
|---|---|---|
| Ely | `staryuuki_krea2_v2` | `Staryuuki, 1woman` |
| Miranda | `miranda_capurro_krea2` | `1woman, miranda_capurro` |
| María | `karen_krea2.safetensors` | `karen, 1woman` (la metadata dice `karen_character`, pero el usuario confirmó `karen`) |
| Pamela | `SadieS_SoloLoRA_Kr2v1.safetensors` | `sadie, 1woman` (sin metadata; es pelirroja, la peluca rosa fue marketing) |
Para otra web con otros personajes: pedir al usuario quién es quién y su palabra; leer metadata con `struct`+`json` del `.safetensors` (`ss_tag_frequency`).

## 3. Cómo se generan buenas imágenes
- Estilo en el prompt **descrito con palabras** ("retro 2010s candy-land summer pop aesthetic, bubblegum pink and pastel cotton-candy sky blue, vaporwave pastel gradient, sparkles, high-key dreamy lighting, fluffy pink clouds, pastel balloons, pink scrub top, playful pose, hand on hip, wink"). **Nombrar a una cantante ("Katy Perry California Gurls") hizo que el modelo copiara su cara por encima del LoRA.** Nunca nombres personas reales.
- Poner el **color de pelo explícito**: el "pink" del estilo se filtra al pelo y rompe la identidad.
- Vestuario: "modest high-collar ... neckline fully covered, no cleavage" cuando el modelo abre escotes (el "fully clothed" solo no bastó).
- Tratamientos de spa: "masseuse ... massaging a client lying face-down on a pink massage bed, covered by a thick pink towel up to the shoulders, face not visible". Para dúo (4 manos) la 2ª mujer sale con rasgos de la 1ª; aceptable.
- Formatos: retratos 576×1024 (primera pasada → reducir a 576×1024 webp q86), escenas 768×432 → 1280×720 webp q84 (~80 KB).
- Convertir con `magick x.png -quality 82 x.webp` y **borrar los PNG** de `public/`.

## 4. Trampas de código ya encontradas
- **Shader preexistente roto**: `taylorInvSqrt(vec3)` llamado con vec3 pero definido solo para vec4 → el fondo nunca compilaba. Ante pantalla negra/rara, mirar la consola (`m.type()==='error'`, mensaje completo sin recortar).
- **Remapeo masivo de colores** (oscuro→pastel) con regex sobre SCSS: funciona (fondos `rgba(17,10,30,x)`→`rgba(255,240,248,x)`, `color:#fff`→`var(--color-text)`), pero **rompe** (a) botones con gradiente (texto pasa a oscuro → volver a `#fff`), (b) degradados de overlay que luego tu `replace` ya no encuentra (reescribe la regla completa), (c) zonas que deben seguir oscuras (admin): redefinir tokens en `:host { --color-text:#fff8fa; ... }` solo ahí.
- **Grid de 12 columnas en móvil**: con `gap` grande un elemento `span 12` mide solo la suma de gaps. En ≤1100px → `grid-template-columns: minmax(0,1fr)` y `grid-column:auto`; hijos con `min-width:0`.
- Titulares: tamaños en `vw` con `clamp()` (11,8vw hero, 7,4vw h2 en ≤480px); `overflow-wrap:anywhere` parte palabras feo → solo `normal` + tamaño menor.
- `100vh`→`100svh` (barra del navegador móvil); `viewport-fit=cover`.
- Hero con GSAP: no partir letras si el título usa `background-clip:text` (se pierde el degradado); animar el bloque entero.
- Splash/intro de video de 15 s: limitar (7 s), omitir con `saveData`/2G/reduced-motion, y que la entrada del hero espere el evento `recovr:splash-done`.
- `three` + `@defer (on idle)` en el orbe y en el fondo fluido: bundle inicial 946→404 kB.

## 5. Optimización móvil (checklist con evidencia)
- Fondo fluido a `pixelRatio` 0,5 en táctil (1 en desktop), menos partículas, `antialias:false`, pausa con `document.hidden`.
- Directiva `reference/in-view-video.directive.ts`: en `(hover:none)` el video de la tarjeta se reproduce solo si ≥60 % visible. Tilt y cursor personalizado desactivados en `(hover:none),(pointer:coarse)`.
- Videos H.264 `-an -crf 26 -preset slow -pix_fmt yuv420p -movflags +faststart` (~600 KB para 7 s a 960×544). Borrar videos pesados no usados (había uno de 14 MB).
- Auditar con `scripts/responsive_audit.mjs` (viewports 360/390/768/1024/1440/1920, emulación táctil, detecta `scrollWidth>innerWidth` y elementos que sobresalen; ignora marquesinas/capas recortadas a propósito) y `scripts/modal_and_touch_video_check.mjs`.
- `npx ng build` (producción) debe compilar sin errores ni warning de presupuesto.

## 6. Verificación visual (sin GPU en el navegador headless)
- Chromium del sistema: `executablePath: '/usr/bin/chromium'` (Playwright no trae navegador descargado) y flags `--use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader --ignore-gpu-blocklist --autoplay-policy=no-user-gesture-required`. `npm i playwright` está en el proyecto; los scripts importan `playwright` (ajustar la ruta si hace falta).
- Saltar el splash: `addInitScript(() => sessionStorage.setItem('recovr_visited','1'))` (o clic en "Saltar").
- `scripts/scroll_pin_shots.mjs`: capturas en 4 puntos del tramo pinned. Armar `montage` con ImageMagick y leerlo con Read. En `montage`, `-resize` va con tamaño explícito (un `-resize 900x` de un solo archivo puede dar miniatura).

## 7. Plantilla de entrega al usuario
Link `http://localhost:4200`, qué cambió (fases), qué queda pendiente/por decidir, advertencias honestas (probado en emulación, no en dispositivo real), y qué decisión necesitas de él. Guardar en memoria del proyecto el mapa personaje→LoRA y el estado de la rama.
