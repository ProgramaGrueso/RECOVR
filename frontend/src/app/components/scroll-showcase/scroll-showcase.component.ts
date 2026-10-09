// Sección anclada con scroll narrativo: retratos que se revelan, fondo que cambia de escena.
import { Component, ElementRef, ViewChild, AfterViewInit, OnDestroy, NgZone, inject, ChangeDetectionStrategy } from '@angular/core';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

@Component({
  selector: 'app-scroll-showcase',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './scroll-showcase.component.html',
  styleUrls: ['./scroll-showcase.component.scss']
})
export class ScrollShowcaseComponent implements AfterViewInit, OnDestroy {
  @ViewChild('root', { static: true }) private root!: ElementRef<HTMLElement>;
  private zone = inject(NgZone);
  private ctx?: gsap.Context;

  ngAfterViewInit(): void {
    gsap.registerPlugin(ScrollTrigger);
    this.zone.runOutsideAngular(() => {
      const el = this.root.nativeElement;
      const q = (s: string) => el.querySelector(s);
      const qa = (s: string) => el.querySelectorAll(s);
      const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (reduced) { el.classList.add('is-static'); return; }

      this.ctx = gsap.context(() => {
        const mm = gsap.matchMedia();
        mm.add('(min-width: 769px)', () => {
          const tl = gsap.timeline({
            defaults: { ease: 'none' },
            scrollTrigger: { trigger: el, start: 'top top', end: '+=320%', scrub: 0.8, pin: true, anticipatePin: 1 }
          });
          tl.fromTo(qa('.sc-line'), { yPercent: 110 }, { yPercent: 0, stagger: 0.12, duration: 0.5, ease: 'power3.out' }, 0)
            .fromTo(q('.sc-p-left'), { clipPath: 'inset(100% 0 0 0)', yPercent: 12 }, { clipPath: 'inset(0% 0 0 0)', yPercent: 0, duration: 1 }, 0.1)
            .fromTo(q('.sc-p-right'), { clipPath: 'inset(100% 0 0 0)', yPercent: 24 }, { clipPath: 'inset(0% 0 0 0)', yPercent: 0, duration: 1 }, 0.3)
            .to(q('.sc-p-left'), { yPercent: -10, duration: 1.4 }, 1)
            .to(q('.sc-p-right'), { yPercent: -22, duration: 1.4 }, 1)
            .to(q('.sc-bg-a'), { opacity: 0, scale: 1.12, duration: 0.8 }, 1.6)
            .fromTo(q('.sc-bg-b'), { opacity: 0, scale: 1.2 }, { opacity: 1, scale: 1, duration: 0.8 }, 1.6)
            .to(qa('.sc-p'), { opacity: 0, scale: 0.92, duration: 0.6 }, 2.0)
            .to(q('.sc-title'), { opacity: 0, yPercent: -12, duration: 0.6 }, 1.9)
            .fromTo(q('.sc-wide'), { clipPath: 'inset(50% 50% 50% 50% round 24px)' }, { clipPath: 'inset(0% 0% 0% 0% round 0px)', duration: 1 }, 2.1)
            .fromTo(qa('.sc-out'), { opacity: 0, y: 40 }, { opacity: 1, y: 0, stagger: 0.15, duration: 0.5, ease: 'power2.out' }, 2.8);
        });
        mm.add('(max-width: 768px)', () => {
          gsap.from(qa('.sc-p'), { y: 80, opacity: 0, stagger: 0.2, duration: 1, ease: 'expo.out', scrollTrigger: { trigger: el, start: 'top 70%' } });
          gsap.from(qa('.sc-line'), { yPercent: 110, stagger: 0.1, duration: 1, ease: 'expo.out', scrollTrigger: { trigger: el, start: 'top 75%' } });
        });
      }, el);
    });
  }

  ngOnDestroy(): void {
    this.ctx?.revert();
  }
}
