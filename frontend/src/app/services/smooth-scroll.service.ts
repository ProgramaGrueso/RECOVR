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

  scrollTo(target: string | number | HTMLElement): void {
    this.lenis?.scrollTo(target as any);
  }
}
