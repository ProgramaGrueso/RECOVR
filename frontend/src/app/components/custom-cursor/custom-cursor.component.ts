// src/app/components/custom-cursor/custom-cursor.component.ts
import {
  Component,
  ElementRef,
  OnInit,
  OnDestroy,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  NgZone,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { CursorService, CursorState } from '../../services/cursor.service';

@Component({
  selector: 'app-custom-cursor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div
      #cursorWrapper
      class="custom-cursor-wrapper"
      [class.visible]="isVisible"
      [class.active]="isHovered || state.active">
      <div class="cursor-dot"></div>
      <div class="cursor-ring"></div>
      <div
        #cursorLabel
        class="cursor-label"
        [class.has-text]="!!displayLabel">
        {{ displayLabel }}
      </div>
    </div>
  `,
  styleUrls: ['./custom-cursor.component.scss']
})
export class CustomCursorComponent implements OnInit, OnDestroy {
  @ViewChild('cursorWrapper', { static: true })
  private cursorWrapperRef!: ElementRef<HTMLDivElement>;

  @ViewChild('cursorLabel', { static: true })
  private cursorLabelRef!: ElementRef<HTMLDivElement>;

  private cursorService = inject(CursorService);
  private ngZone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);

  state: CursorState = { active: false, label: '' };
  isVisible = false;
  isHovered = false;
  currentDelegatedLabel = '';

  get displayLabel(): string {
    return this.state.label || this.currentDelegatedLabel;
  }

  private mouseX = -100;
  private mouseY = -100;
  private currentX = -100;
  private currentY = -100;
  private rafId: number | null = null;
  private sub?: Subscription;

  private boundOnMouseMove = this.onMouseMove.bind(this);
  private boundOnMouseOver = this.onMouseOver.bind(this);
  private boundOnMouseOut = this.onMouseOut.bind(this);
  private boundOnMouseLeaveDoc = this.onMouseLeaveDoc.bind(this);
  private boundOnMouseEnterDoc = this.onMouseEnterDoc.bind(this);

  ngOnInit(): void {
    // Sincronización con llamadas programáticas a CursorService
    this.sub = this.cursorService.state$.subscribe(s => {
      this.state = s;
      if (s.active) {
        this.isHovered = true;
      } else if (!this.currentDelegatedLabel) {
        this.isHovered = false;
      }
      this.cdr.markForCheck();
    });

    if (typeof window !== 'undefined') {
      this.ngZone.runOutsideAngular(() => {
        window.addEventListener('mousemove', this.boundOnMouseMove, { passive: true });
        document.addEventListener('mouseover', this.boundOnMouseOver, { passive: true });
        document.addEventListener('mouseout', this.boundOnMouseOut, { passive: true });
        document.addEventListener('mouseleave', this.boundOnMouseLeaveDoc);
        document.addEventListener('mouseenter', this.boundOnMouseEnterDoc);

        this.startRenderLoop();
      });
    }
  }

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('mousemove', this.boundOnMouseMove);
      document.removeEventListener('mouseover', this.boundOnMouseOver);
      document.removeEventListener('mouseout', this.boundOnMouseOut);
      document.removeEventListener('mouseleave', this.boundOnMouseLeaveDoc);
      document.removeEventListener('mouseenter', this.boundOnMouseEnterDoc);
    }

    if (this.rafId !== null) {
      cancelAnimationFrame(this.rafId);
    }

    this.sub?.unsubscribe();
  }

  private onMouseMove(e: MouseEvent): void {
    this.mouseX = e.clientX;
    this.mouseY = e.clientY;

    if (!this.isVisible) {
      this.isVisible = true;
      const el = this.cursorWrapperRef?.nativeElement;
      if (el) el.classList.add('visible');
    }
  }

  private onMouseLeaveDoc(): void {
    this.isVisible = false;
    const el = this.cursorWrapperRef?.nativeElement;
    if (el) el.classList.remove('visible');
  }

  private onMouseEnterDoc(): void {
    this.isVisible = true;
    const el = this.cursorWrapperRef?.nativeElement;
    if (el) el.classList.add('visible');
  }

  // Delegación de eventos global para elementos con atributo [data-cursor]
  private onMouseOver(e: MouseEvent): void {
    const target = (e.target as HTMLElement).closest('[data-cursor]') as HTMLElement | null;
    if (target) {
      const label = target.getAttribute('data-cursor') || '';
      this.currentDelegatedLabel = label;
      this.isHovered = true;

      const el = this.cursorWrapperRef?.nativeElement;
      const lbl = this.cursorLabelRef?.nativeElement;
      if (el) el.classList.add('active');
      if (lbl) {
        lbl.textContent = label;
        if (label) lbl.classList.add('has-text');
      }
    }
  }

  private onMouseOut(e: MouseEvent): void {
    const target = (e.target as HTMLElement).closest('[data-cursor]') as HTMLElement | null;
    if (target) {
      this.currentDelegatedLabel = '';
      if (!this.state.active) {
        this.isHovered = false;
        const el = this.cursorWrapperRef?.nativeElement;
        if (el) el.classList.remove('active');
      }

      const lbl = this.cursorLabelRef?.nativeElement;
      if (lbl && !this.state.label) {
        lbl.textContent = '';
        lbl.classList.remove('has-text');
      }
    }
  }

  // Bucle de renderizado con física elástica (lerp spring) desacoplado a 60 FPS
  private startRenderLoop = (): void => {
    const ease = (this.isHovered || this.state.active) ? 0.22 : 0.16;
    this.currentX += (this.mouseX - this.currentX) * ease;
    this.currentY += (this.mouseY - this.currentY) * ease;

    const el = this.cursorWrapperRef?.nativeElement;
    if (el) {
      el.style.transform = `translate3d(${this.currentX}px, ${this.currentY}px, 0) translate(-50%, -50%)`;
    }

    this.rafId = requestAnimationFrame(this.startRenderLoop);
  };
}
