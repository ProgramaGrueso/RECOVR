import { Component, EventEmitter, Output, ViewChild, ElementRef, AfterViewInit, OnInit, inject } from '@angular/core';
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
export class HeroComponent implements OnInit, AfterViewInit {
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);

  @Output() reserveClick = new EventEmitter<void>();
  @ViewChild('heroVideo') heroVideoRef?: ElementRef<HTMLVideoElement>;

  isVideoFailed = false;
  videoSource = 'assets/Video_00025.mp4';
  showVideo = true;
  
  ngOnInit(): void {
    const hasSeenVideo = sessionStorage.getItem('recovr_has_seen_video');
    if (hasSeenVideo) {
      this.showVideo = false;
    } else {
      sessionStorage.setItem('recovr_has_seen_video', 'true');
    }
  }

  ngAfterViewInit(): void {
    const video = this.heroVideoRef?.nativeElement;
    if (video) {
      const tryPlay = () => {
        const playPromise = video.play();
        if (playPromise !== undefined) {
          playPromise.catch(() => {
            this.isVideoFailed = true;
          });
        }
      };

      if (video.readyState >= 3) {
        tryPlay();
      } else {
        video.addEventListener('loadeddata', tryPlay);
        video.addEventListener('canplay', tryPlay);
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

  setCursor(label: string) {
    this.cursorService.setCursor(label, true, 'default');
  }

  resetCursor() {
    this.cursorService.resetCursor();
  }
}
