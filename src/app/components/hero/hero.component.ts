import { Component, EventEmitter, Output, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../services/booking.service';

@Component({
  selector: 'app-hero',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './hero.component.html',
  styleUrls: ['./hero.component.scss']
})
export class HeroComponent implements AfterViewInit {
  @Output() reserveClick = new EventEmitter<void>();
  @ViewChild('heroVideo') heroVideoRef?: ElementRef<HTMLVideoElement>;

  isVideoFailed = false;

  constructor(private bookingService: BookingService) {}

  ngAfterViewInit(): void {
    const video = this.heroVideoRef?.nativeElement;
    if (video) {
      const playPromise = video.play();
      if (playPromise !== undefined) {
        playPromise.catch(() => {
          // Autoplay fue bloqueado por el navegador o modo ahorro de datos
          this.isVideoFailed = true;
        });
      }
    }
  }

  onVideoError(): void {
    this.isVideoFailed = true;
  }

  onReserve(): void {
    this.bookingService.navigateToBooking();
    this.reserveClick.emit();
  }
}

