import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, EventEmitter, Input, NgZone, OnChanges, OnInit, Output,
  QueryList, ViewChildren, computed, inject, signal
} from '@angular/core';
import { ServiceItem } from '../../models/service.model';

export interface FiltroTratamiento { key: string; label: string; }

const pad = (n: number) => String(n).padStart(2, '0');

/**
 * Carrusel interactivo de tratamientos (el mismo de "Nuestras habitaciones" de Wimbledon): tarjetas llave sobre
 * una pantalla CRT con el tratamiento central como foco. Se mueve con flechas del teclado, clic, botones o
 * deslizando; Enter o clic en la tarjeta central abre el tratamiento. La tarjeta central reproduce su video.
 */
@Component({
  selector: 'app-tratamientos-carrusel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tratamientos-carrusel.component.html',
  styleUrls: ['./tratamientos-carrusel.component.scss'],
  host: { '[class.bn]': 'blancoYNegro()' }
})
export class TratamientosCarruselComponent implements OnInit, OnChanges {
  @Input() servicios: ServiceItem[] = [];
  @Input() filtros: FiltroTratamiento[] = [];
  @Output() abrir = new EventEmitter<ServiceItem>();
  @Output() reservar = new EventEmitter<ServiceItem>();

  @ViewChildren('vid') private videos!: QueryList<ElementRef<HTMLVideoElement>>;

  private host = inject(ElementRef<HTMLElement>);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);

  readonly filtro = signal('ALL');
  readonly actual = signal(0);
  readonly blancoYNegro = signal(false);
  readonly reloj = signal('00:00:00');
  private readonly todos = signal<ServiceItem[]>([]);

  readonly lista = computed(() => {
    const f = this.filtro();
    return f === 'ALL' ? this.todos() : this.todos().filter(s => this.coincide(s, f));
  });
  readonly activo = computed(() => this.lista()[this.actual()] ?? null);

  private visible = false;
  private inicioToque: number | null = null;
  readonly pad = pad;

  ngOnChanges(): void {
    this.todos.set(this.servicios);
    if (this.actual() >= this.lista().length) this.actual.set(0);
  }

  ngOnInit(): void {
    const el = this.host.nativeElement as HTMLElement;
    // El teclado, el reloj y el video solo trabajan mientras la sección está a la vista
    const obs = new IntersectionObserver(([e]) => {
      this.visible = e.intersectionRatio > 0.35;
      this.sincronizarVideo();
    }, { threshold: [0, 0.35, 1] });
    obs.observe(el);

    const teclas = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement | null;
      if (!this.visible || t?.closest('input, textarea, select, [contenteditable]')) return;
      if (document.querySelector('.cinema-modal.open, .video-modal-backdrop, .asistente__panel:focus-within')) return;
      if (e.key === 'ArrowRight') this.zone.run(() => this.ir(1));
      else if (e.key === 'ArrowLeft') this.zone.run(() => this.ir(-1));
      else if (e.key === 'Enter' && !t?.closest('button, a')) this.zone.run(() => { const s = this.activo(); if (s) this.abrir.emit(s); });
    };
    document.addEventListener('keydown', teclas);

    const t0 = Date.now();
    const intervalo = matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : window.setInterval(() => {
      if (!this.visible || document.hidden) return;
      const s = Math.floor((Date.now() - t0) / 1000);
      this.reloj.set(`${pad(Math.floor(s / 3600))}:${pad(Math.floor(s / 60) % 60)}:${pad(s % 60)}`);
    }, 1000);

    this.destroyRef.onDestroy(() => {
      obs.disconnect();
      document.removeEventListener('keydown', teclas);
      if (intervalo) clearInterval(intervalo);
    });
  }

  contar(key: string): number {
    return key === 'ALL' ? this.todos().length : this.todos().filter(s => this.coincide(s, key)).length;
  }

  filtrar(key: string): void {
    this.filtro.set(key);
    this.actual.set(0);
    this.sincronizarVideo();
  }

  ir(paso: number): void {
    const n = this.lista().length;
    if (!n) return;
    this.actual.set((this.actual() + paso + n) % n);
    this.sincronizarVideo();
  }

  /** Distancia de una tarjeta al centro, dando la vuelta por el lado más corto. */
  distancia(i: number): number {
    const n = this.lista().length;
    let d = i - this.actual();
    if (d > n / 2) d -= n;
    if (d < -n / 2) d += n;
    return d;
  }

  alClicTarjeta(i: number, s: ServiceItem): void {
    if (i === this.actual()) this.abrir.emit(s);
    else { this.actual.set(i); this.sincronizarVideo(); }
  }

  alternarBn(): void {
    this.blancoYNegro.update(v => !v);
  }

  toqueInicio(e: TouchEvent): void {
    this.inicioToque = e.touches[0].clientX;
  }

  toqueFin(e: TouchEvent): void {
    if (this.inicioToque === null) return;
    const delta = e.changedTouches[0].clientX - this.inicioToque;
    if (Math.abs(delta) > 40) this.ir(delta < 0 ? 1 : -1);
    this.inicioToque = null;
  }

  /** Solo la tarjeta central reproduce su video (silenciado), y solo con la sección a la vista. */
  private sincronizarVideo(): void {
    setTimeout(() => {
      const calmado = matchMedia('(prefers-reduced-motion: reduce)').matches;
      this.videos?.forEach(ref => {
        const v = ref.nativeElement;
        const central = Number(v.dataset['i']) === this.actual();
        if (central && this.visible && !calmado) {
          v.muted = true;
          v.play().catch(() => {});
        } else if (!v.paused) {
          v.pause();
        }
      });
    });
  }

  private coincide(s: ServiceItem, key: string): boolean {
    return !!s.category && s.category.toUpperCase().includes(key.toUpperCase());
  }
}
