import { Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';

@Component({
  selector: 'app-hero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hero.component.html',
  styleUrls: ['./hero.component.scss']
})
export class HeroComponent {
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);

  @Output() reserveClick = new EventEmitter<void>();

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
