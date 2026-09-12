import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Professional } from '../../models/professional.model';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';

@Component({
  selector: 'app-professional-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './professional-card.component.html',
  styleUrls: ['./professional-card.component.scss']
})
export class ProfessionalCardComponent {
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);

  @Input() professional!: Professional;
  @Output() selectProfessional = new EventEmitter<Professional>();

  @ViewChild('profVideo') profVideoRef?: ElementRef<HTMLVideoElement>;

  isHovered = false;

  onMouseEnter(): void {
    this.isHovered = true;
    this.setCursor();
    const video = this.profVideoRef?.nativeElement;
    if (video) {
      video.muted = false;
      video.volume = 1.0;
      const playPromise = video.play();
      if (playPromise !== undefined) {
        playPromise.catch(() => {
          // Si el navegador requiere interacción previa para audio desmuteado
          if (video.muted === false) {
            video.muted = true;
            video.play().catch(() => {});
          }
        });
      }
    }
  }

  onMouseLeave(): void {
    this.isHovered = false;
    this.resetCursor();
    const video = this.profVideoRef?.nativeElement;
    if (video) {
      video.pause();
    }
  }

  onSelect(): void {
    this.bookingService.navigateToBooking();
    this.selectProfessional.emit(this.professional);
  }

  getFirstNameUppercase(name: string): string {
    if (!name) return '';
    return name.split(' ')[0].toUpperCase();
  }

  setCursor(label?: string) {
    const text = label || `[ ${this.getFirstNameUppercase(this.professional?.name)} ]`;
    this.cursorService.setCursor(text, true, 'view');
  }

  resetCursor() {
    this.cursorService.resetCursor();
  }
}
