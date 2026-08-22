import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ServiceItem } from '../../models/service.model';
import { BookingService } from '../../services/booking.service';

@Component({
  selector: 'app-service-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './service-card.component.html',
  styleUrls: ['./service-card.component.scss']
})
export class ServiceCardComponent {
  @Input() service?: ServiceItem;
  @Input() isLoading = false;
  @Output() reserve = new EventEmitter<ServiceItem>();

  constructor(private bookingService: BookingService) {}

  onReserve(): void {
    if (this.service) {
      this.bookingService.navigateToBooking(this.service);
      this.reserve.emit(this.service);
    }
  }
}

