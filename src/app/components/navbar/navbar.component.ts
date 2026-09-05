import { Component, EventEmitter, HostListener, Output, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';
import { AuthService } from '../../services/auth.service';
import { BrandMarkComponent } from '../brand-mark/brand-mark.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, BrandMarkComponent],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);
  auth = inject(AuthService);

  currentUser = computed(() => this.auth.currentUser());
  clientPoints = computed(() => this.auth.getClientPoints());

  @Output() reserveClick = new EventEmitter<void>();
  isMobileMenuOpen = false;
  isScrolled = false;

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled = window.scrollY > 40;
  }

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

  logout(): void {
    this.closeMobileMenu();
    this.auth.logout();
  }

  setCursor(label: string) {
    this.cursorService.setCursor(label, true, 'default');
  }

  resetCursor() {
    this.cursorService.resetCursor();
  }
}
