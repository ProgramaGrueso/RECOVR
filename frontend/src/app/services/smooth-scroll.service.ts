import { Injectable, NgZone, inject } from '@angular/core';
import Lenis from 'lenis';

/** Scroll inercial global (Lenis). Se desactiva con prefers-reduced-motion. */
@Injectable({ providedIn: 'root' })
export class SmoothScrollService {
  private zone = inject(NgZone);
  private lenis?: Lenis;

  start(): void {
    if (this.lenis || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    this.zone.runOutsideAngular(() => {
      this.lenis = new Lenis({ lerp: 0.085, wheelMultiplier: 0.9 });
      const raf = (t: number) => { this.lenis!.raf(t); requestAnimationFrame(raf); };
      requestAnimationFrame(raf);
    });
  }

  /** Desplaza hasta el destino; sin Lenis (movimiento reducido) salta directo. `offset` deja aire bajo la barra fija. */
  scrollTo(target: string | number | HTMLElement, offset = 0): void {
    if (this.lenis) {
      this.lenis.scrollTo(target as any, { offset });
      return;
    }
    const el = typeof target === 'string' ? document.querySelector<HTMLElement>(target) : target instanceof HTMLElement ? target : null;
    const y = typeof target === 'number' ? target : el ? el.getBoundingClientRect().top + scrollY + offset : null;
    if (y !== null) window.scrollTo({ top: y });
  }
}
