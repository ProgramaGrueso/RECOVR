import { ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { CursorService } from '../../services/cursor.service';

const TEXTO = 'input:not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit]), textarea, select, [contenteditable="true"]';
const CLICABLE = 'a, button, [role="button"], [role="option"], label, summary, [data-cursor]';

/**
 * Cursor gatito (traído del proyecto Wimbledon): Maxwell the Cat girando y balanceándose, siguiendo al mouse.
 * El sprite es un WebP animado recortado del video del meme (proyecto educativo, sin uso comercial).
 *
 * Los navegadores no animan el cursor nativo, así que se oculta y el gato se dibuja en un elemento fijo.
 * Solo con mouse real: en pantallas táctiles no se activa. Con "reducir movimiento" usa un cuadro fijo y sin estela.
 * Sobre campos de texto vuelve el cursor de texto nativo.
 *
 * Extras: sigue con un resorte corto (el punto de clic es el centro del gato), se inclina según la velocidad,
 * deja una estela de burbujas rosa/lila/cian, suelta una ráfaga al hacer clic y muestra la etiqueta
 * de los elementos con [data-cursor] (o la que fije CursorService), como hacía el cursor anterior de RECOVR.
 */
@Component({
  selector: 'app-cat-cursor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <canvas #trail class="cat-trail" aria-hidden="true"></canvas>
    <div #cat class="cat-cursor" aria-hidden="true">
      <div #tilt class="cat-cursor__tilt">
        <div class="cat-cursor__sway"><div class="cat-cursor__spin"></div></div>
      </div>
      <span #label class="cat-cursor__label"></span>
    </div>
  `,
  styleUrls: ['./cat-cursor.component.scss']
})
export class CatCursorComponent implements OnInit, OnDestroy {
  @ViewChild('cat', { static: true }) private catRef!: ElementRef<HTMLDivElement>;
  @ViewChild('tilt', { static: true }) private tiltRef!: ElementRef<HTMLDivElement>;
  @ViewChild('label', { static: true }) private labelRef!: ElementRef<HTMLSpanElement>;
  @ViewChild('trail', { static: true }) private trailRef!: ElementRef<HTMLCanvasElement>;

  private zone = inject(NgZone);
  private cursorService = inject(CursorService);
  private sub?: Subscription;
  private raf = 0;
  private quitar: Array<() => void> = [];
  private etiquetaServicio = '';
  private etiquetaDelegada = '';

  ngOnInit(): void {
    if (typeof window === 'undefined' || !matchMedia('(hover: hover) and (pointer: fine)').matches) return;
    this.sub = this.cursorService.state$.subscribe(s => {
      this.etiquetaServicio = s.active ? s.label : '';
      this.pintarEtiqueta();
    });
    this.zone.runOutsideAngular(() => this.iniciar());
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.raf);
    this.quitar.forEach(f => f());
    this.sub?.unsubscribe();
    document.documentElement.classList.remove('has-cat-cursor');
  }

  private pintarEtiqueta(): void {
    const texto = this.etiquetaServicio || this.etiquetaDelegada;
    const el = this.labelRef.nativeElement;
    el.textContent = texto;
    el.classList.toggle('visible', !!texto);
  }

  private escuchar<K extends keyof DocumentEventMap>(obj: Document | HTMLElement, tipo: K, fn: (e: DocumentEventMap[K]) => void, opciones?: AddEventListenerOptions): void {
    obj.addEventListener(tipo, fn as EventListener, opciones);
    this.quitar.push(() => obj.removeEventListener(tipo, fn as EventListener, opciones));
  }

  private iniciar(): void {
    const calmado = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const el = this.catRef.nativeElement;
    const tilt = this.tiltRef.nativeElement;
    const lienzo = this.trailRef.nativeElement;
    const ctx = lienzo.getContext('2d');
    document.documentElement.classList.add('has-cat-cursor');

    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const ajustar = () => {
      lienzo.width = innerWidth * dpr;
      lienzo.height = innerHeight * dpr;
      ctx?.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    ajustar();
    window.addEventListener('resize', ajustar, { passive: true });
    this.quitar.push(() => window.removeEventListener('resize', ajustar));

    let objX = -200, objY = -200, x = -200, y = -200, vx = 0, vy = 0, ang = 0, visible = false;
    type Chispa = { x: number; y: number; vx: number; vy: number; vida: number; max: number; r: number; c: string };
    const chispas: Chispa[] = [];
    const colores = ['255,42,133', '255,110,170', '168,85,247', '23,174,224', '255,255,255'];

    const soltar = (px: number, py: number, n: number, fuerza: number, vida: number) => {
      for (let i = 0; i < n; i++) {
        const a = Math.random() * Math.PI * 2;
        const v = Math.random() * fuerza;
        chispas.push({
          x: px, y: py, vx: Math.cos(a) * v, vy: Math.sin(a) * v - 0.4,
          vida, max: vida, r: 1 + Math.random() * 2.2, c: colores[(Math.random() * colores.length) | 0],
        });
      }
    };

    let ultimo = performance.now();
    const bucle = (t: number) => {
      const dt = Math.min(48, t - ultimo) / 16.67;
      ultimo = t;
      // Seguimiento con resorte amortiguado: rápido para no estorbar al hacer clic
      const nx = x + (objX - x) * Math.min(1, 0.42 * dt);
      const ny = y + (objY - y) * Math.min(1, 0.42 * dt);
      vx = nx - x; vy = ny - y;
      x = nx; y = ny;
      el.style.transform = `translate3d(${x}px, ${y}px, 0)`;
      // Inclinación según la velocidad horizontal, con retorno suave
      const meta = Math.max(-22, Math.min(22, vx * 1.6));
      ang += (meta - ang) * Math.min(1, 0.18 * dt);
      tilt.style.transform = `rotate(${ang.toFixed(2)}deg)`;

      if (!calmado && ctx) {
        if (visible && Math.hypot(vx, vy) > 1.2 && Math.random() < 0.7) soltar(x, y + 6, 1, 0.7, 34 + Math.random() * 22);
        ctx.clearRect(0, 0, innerWidth, innerHeight);
        for (let i = chispas.length - 1; i >= 0; i--) {
          const p = chispas[i];
          p.vida -= dt;
          if (p.vida <= 0) { chispas.splice(i, 1); continue; }
          p.x += p.vx * dt; p.y += p.vy * dt; p.vy -= 0.012 * dt; p.vx *= 0.985;
          const k = p.vida / p.max;
          const radio = p.r * 4 * k + 1;
          const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, radio);
          g.addColorStop(0, `rgba(${p.c},${(0.9 * k).toFixed(3)})`);
          g.addColorStop(1, `rgba(${p.c},0)`);
          ctx.fillStyle = g;
          ctx.beginPath();
          ctx.arc(p.x, p.y, radio, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      this.raf = requestAnimationFrame(bucle);
    };
    this.raf = requestAnimationFrame(bucle);

    this.escuchar(document, 'mousemove', (e: MouseEvent) => {
      if (!visible) { x = objX = e.clientX; y = objY = e.clientY; }
      objX = e.clientX;
      objY = e.clientY;
      visible = true;
      el.classList.add('visible');
      const destino = e.target instanceof Element ? e.target : null;
      el.classList.toggle('sobre-texto', !!destino?.closest(TEXTO));
      el.classList.toggle('sobre-enlace', !!destino?.closest(CLICABLE));
      const etiqueta = destino?.closest('[data-cursor]')?.getAttribute('data-cursor') ?? '';
      if (etiqueta !== this.etiquetaDelegada) {
        this.etiquetaDelegada = etiqueta;
        this.pintarEtiqueta();
      }
    }, { passive: true });

    // Sobre un iframe (los ositos) el documento deja de recibir el mouse: el gato se esconde y el iframe usa su cursor
    this.escuchar(document, 'mouseover', (e: MouseEvent) => {
      if (!(e.target instanceof HTMLIFrameElement) || getComputedStyle(e.target).pointerEvents === 'none') return;
      visible = false;
      el.classList.remove('visible');
      if (this.etiquetaDelegada) { this.etiquetaDelegada = ''; this.pintarEtiqueta(); }
    }, { passive: true });

    this.escuchar(document, 'mousedown', () => {
      el.classList.add('click');
      if (!calmado) soltar(objX, objY, 22, 4.2, 40);
    });
    this.escuchar(document, 'mouseup', () => el.classList.remove('click'));
    this.escuchar(document.documentElement, 'mouseleave', () => { visible = false; el.classList.remove('visible'); });
  }
}
