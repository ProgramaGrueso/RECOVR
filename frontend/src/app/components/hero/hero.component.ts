import { Component, ElementRef, EventEmitter, Output, AfterViewInit, ViewChild, inject } from '@angular/core';
import gsap from 'gsap';
import { HeroOrbComponent } from '../hero-orb/hero-orb.component';
import { CommonModule } from '@angular/common';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';

@Component({
  selector: 'app-hero',
  standalone: true,
  imports: [CommonModule, HeroOrbComponent],
  templateUrl: './hero.component.html',
  styleUrls: ['./hero.component.scss']
})
export class HeroComponent implements AfterViewInit {
  @ViewChild('heroEl', { static: true }) private heroEl!: ElementRef<HTMLElement>;
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);

  @Output() reserveClick = new EventEmitter<void>();

  ngAfterViewInit(): void {
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const root = this.heroEl.nativeElement;
    const tl = gsap.timeline({ paused: true, delay: 0.2, defaults: { ease: 'expo.out' } });
    // la entrada espera a que termine la intro (splash) si está activa
    if ((window as any).__recovrSplash) addEventListener('recovr:splash-done', () => tl.play(), { once: true });
    else tl.play();
    tl.from(root.querySelector('.hero-top-info'), { y: 20, opacity: 0, duration: 1 })
      .from(root.querySelector('.hero-main-title'), { y: 120, opacity: 0, filter: 'blur(24px)', letterSpacing: '0.12em', duration: 1.8 }, '<0.1')
      .from(root.querySelectorAll('.hero-sub-grid > *'), { y: 40, opacity: 0, duration: 1.2, stagger: 0.12 }, '<0.5');
    // el título se hunde y se desenfoca al hacer scroll
    const onScroll = () => {
      const p = Math.min(scrollY / innerHeight, 1);
      const t = root.querySelector('.hero-main-title') as HTMLElement | null;
      if (t) t.style.transform = `translate3d(0, ${p * -60}px, 0) scale(${1 - p * 0.06})`;
    };
    addEventListener('scroll', onScroll, { passive: true });
  }

  onReserve(): void {
    this.bookingService.navigateToBooking();
    this.reserveClick.emit();
  }

  setCursor(label: string) {
    this.cursorService.setCursor(label, true, 'default');
  }

  resetCursor() {
    this.cursorService.resetCursor();
  }
}
