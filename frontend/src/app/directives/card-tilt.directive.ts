import { Directive, ElementRef, OnInit, OnDestroy, NgZone, Input, inject } from '@angular/core';

@Directive({
  selector: '[appCardTilt]',
  standalone: true
})
export class CardTiltDirective implements OnInit, OnDestroy {
  private el = inject(ElementRef<HTMLElement>);
  private ngZone = inject(NgZone);

  @Input() maxRotation = 7; // Grados máximos de rotación en ejes X/Y
  @Input() elevationY = -6; // Elevación vertical en hover (px)
  @Input() perspective = 1000; // Perspectiva 3D en px

  private boundOnMouseMove = this.onMouseMove.bind(this);
  private boundOnMouseLeave = this.onMouseLeave.bind(this);

  ngOnInit(): void {
    if (typeof window === 'undefined') return;

    const nativeEl = this.el.nativeElement;
    nativeEl.style.transition = 'transform 0.15s ease-out';
    nativeEl.style.transformStyle = 'preserve-3d';

    this.ngZone.runOutsideAngular(() => {
      nativeEl.addEventListener('mousemove', this.boundOnMouseMove, { passive: true });
      nativeEl.addEventListener('mouseleave', this.boundOnMouseLeave, { passive: true });
    });
  }

  ngOnDestroy(): void {
    if (typeof window === 'undefined') return;

    const nativeEl = this.el.nativeElement;
    nativeEl.removeEventListener('mousemove', this.boundOnMouseMove);
    nativeEl.removeEventListener('mouseleave', this.boundOnMouseLeave);
  }

  private onMouseMove(e: MouseEvent): void {
    const nativeEl = this.el.nativeElement;
    const rect = nativeEl.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotateX = ((y - centerY) / centerY) * -this.maxRotation;
    const rotateY = ((x - centerX) / centerX) * this.maxRotation;

    nativeEl.style.transform = `perspective(${this.perspective}px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) translateY(${this.elevationY}px)`;
  }

  private onMouseLeave(): void {
    const nativeEl = this.el.nativeElement;
    nativeEl.style.transform = `perspective(${this.perspective}px) rotateX(0deg) rotateY(0deg) translateY(0px)`;
  }
}
