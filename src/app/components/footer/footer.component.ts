import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrandMarkComponent } from '../brand-mark/brand-mark.component';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, BrandMarkComponent],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent {
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);

  onReserve(): void {
    this.bookingService.navigateToBooking();
  }

  setCursor(label: string) {
    this.cursorService.setCursor(label, true, 'default');
  }

  resetCursor() {
    this.cursorService.resetCursor();
  }
}
