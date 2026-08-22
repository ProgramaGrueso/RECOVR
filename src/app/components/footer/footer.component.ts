import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../services/booking.service';

import { BrandMarkComponent } from '../brand-mark/brand-mark.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterLink, BrandMarkComponent],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent {

  @Output() reserveClick = new EventEmitter<void>();

  constructor(private bookingService: BookingService) {}

  onReserve(): void {
    this.bookingService.navigateToBooking();
    this.reserveClick.emit();
  }
}

