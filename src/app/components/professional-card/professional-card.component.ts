import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Professional } from '../../models/professional.model';
import { BookingService } from '../../services/booking.service';

@Component({
  selector: 'app-professional-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './professional-card.component.html',
  styleUrls: ['./professional-card.component.scss']
})
export class ProfessionalCardComponent {
  @Input() professional!: Professional;
  @Output() selectProfessional = new EventEmitter<Professional>();

  constructor(private bookingService: BookingService) {}

  onSelect(): void {
    this.bookingService.navigateToBooking();
    this.selectProfessional.emit(this.professional);
  }
}

