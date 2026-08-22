import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../services/booking.service';

import { BrandMarkComponent } from '../brand-mark/brand-mark.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, BrandMarkComponent],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {

  @Output() reserveClick = new EventEmitter<void>();
  isMobileMenuOpen = false;

  constructor(private bookingService: BookingService) {}

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  onReserve(): void {
    this.closeMobileMenu();
    this.bookingService.navigateToBooking();
    this.reserveClick.emit();
  }
}

