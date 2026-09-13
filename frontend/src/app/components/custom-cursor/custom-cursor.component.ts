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
      [class.active]="state.active">
      <div class="cursor-dot"></div>
      <div class="cursor-label" *ngIf="state.label">
        {{ state.label }}
      </div>
    </div>
  `,
  styleUrls: ['./custom-cursor.component.scss']
})
export class CustomCursorComponent implements OnInit, OnDestroy {
  @ViewChild('cursorWrapper', { static: true })
  private cursorWrapperRef!: ElementRef<HTMLDivElement>;

  private cursorService = inject(CursorService);
  private ngZone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);

  state: CursorState = { active: false, label: '' };
  private sub?: Subscription;
  private boundOnMouseMove = this.onMouseMove.bind(this);

  ngOnInit(): void {
    this.sub = this.cursorService.state$.subscribe(s => {
      this.state = s;
      this.cdr.markForCheck();
    });

    if (typeof window !== 'undefined') {
      this.ngZone.runOutsideAngular(() => {
        window.addEventListener('mousemove', this.boundOnMouseMove, { passive: true });
      });
    }
  }

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('mousemove', this.boundOnMouseMove);
    }
    this.sub?.unsubscribe();
  }

  private onMouseMove(e: MouseEvent): void {
    const el = this.cursorWrapperRef?.nativeElement;
    if (el) {
      el.style.transform = `translate3d(${e.clientX}px, ${e.clientY}px, 0) translate(-50%, -50%)`;
    }
  }
}
