import { Directive, ElementRef, NgZone, OnDestroy, OnInit, inject } from '@angular/core';

/**
 * En dispositivos sin hover (móviles/tablets) el video de la tarjeta se reproduce solo
 * mientras la tarjeta está visible en pantalla; en escritorio lo controla el hover de la tarjeta.
 */
@Directive({ selector: 'video[appInViewVideo]', standalone: true })
export class InViewVideoDirective implements OnInit, OnDestroy {
  private el = inject<ElementRef<HTMLVideoElement>>(ElementRef);
  private zone = inject(NgZone);
  private io?: IntersectionObserver;

  ngOnInit(): void {
    if (typeof window === 'undefined' || !matchMedia('(hover: none)').matches) return;
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const v = this.el.nativeElement;
    v.muted = true;
    v.playsInline = true;
    this.zone.runOutsideAngular(() => {
      this.io = new IntersectionObserver(([e]) => {
        if (e.isIntersecting) {
          if (v.preload === 'none') v.preload = 'metadata';
          v.play().catch(() => {});
        } else {
          v.pause();
        }
      }, { threshold: 0.6 });
      this.io.observe(v);
    });
  }

  ngOnDestroy(): void {
    this.io?.disconnect();
  }
}
