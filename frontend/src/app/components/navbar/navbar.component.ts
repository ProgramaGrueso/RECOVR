import {
  Component,
  EventEmitter,
  OnInit,
  OnDestroy,
  Output,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  NgZone,
  inject,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';
import { AuthService } from '../../services/auth.service';
import { BrandMarkComponent } from '../brand-mark/brand-mark.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink, BrandMarkComponent],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);
  private ngZone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);
  auth = inject(AuthService);

  currentUser = computed(() => this.auth.currentUser());
  clientPoints = computed(() => this.auth.getClientPoints());

  @Output() reserveClick = new EventEmitter<void>();
  isMobileMenuOpen = false;
  isScrolled = false;

  private boundOnScroll = this.onWindowScroll.bind(this);

  ngOnInit(): void {
    if (typeof window !== 'undefined') {
      this.ngZone.runOutsideAngular(() => {
        window.addEventListener('scroll', this.boundOnScroll, { passive: true });
      });
    }
  }

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('scroll', this.boundOnScroll);
    }
  }

  private onWindowScroll(): void {
    const scrolled = window.scrollY > 40;
    if (scrolled !== this.isScrolled) {
      this.isScrolled = scrolled;
      this.cdr.markForCheck();
    }
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
