// src/app/components/custom-cursor/custom-cursor.component.ts
import { Component, HostListener, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CursorService, CursorState } from '../../services/cursor.service';

@Component({
  selector: 'app-custom-cursor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="custom-cursor-wrapper"
      [class.active]="state.active"
      [style.left.px]="x"
      [style.top.px]="y">
      <div class="cursor-dot"></div>
      <div class="cursor-label" *ngIf="state.label">
        {{ state.label }}
      </div>
    </div>
  `,
  styleUrls: ['./custom-cursor.component.scss']
})
export class CustomCursorComponent implements OnInit {
  private cursorService = inject(CursorService);

  x = -100;
  y = -100;
  state: CursorState = { active: false, label: '' };

  ngOnInit(): void {
    this.cursorService.state$.subscribe(s => {
      this.state = s;
    });
  }

  @HostListener('window:mousemove', ['$event'])
  onMouseMove(e: MouseEvent): void {
    this.x = e.clientX;
    this.y = e.clientY;
  }
}
