import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServiceItem } from '../../models/service.model';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';

@Component({
  selector: 'app-service-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './service-card.component.html',
  styleUrls: ['./service-card.component.scss']
})
export class ServiceCardComponent {
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);

  @Input() service?: ServiceItem;
  @Input() isLoading = false;
  @Output() reserve = new EventEmitter<ServiceItem>();

  @ViewChild('cardVideo') cardVideoRef?: ElementRef<HTMLVideoElement>;

  isHovered = false;

  onMouseEnter(): void {
    this.isHovered = true;
    this.setCursor(this.service?.code || 'MASAJE');
    const video = this.cardVideoRef?.nativeElement;
    if (video) {
      video.muted = false;
      video.volume = 1.0;
      const playPromise = video.play();
      if (playPromise !== undefined) {
        playPromise.catch(() => {
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
    const video = this.cardVideoRef?.nativeElement;
    if (video) {
      video.pause();
    }
  }

  onReserve(): void {
    if (this.service) {
      this.bookingService.navigateToBooking(this.service);
      this.reserve.emit(this.service);
    }
  }

  setCursor(label: string) {
    this.cursorService.setCursor(label, true, 'reserve');
  }

  resetCursor() {
    this.cursorService.resetCursor();
  }
}
