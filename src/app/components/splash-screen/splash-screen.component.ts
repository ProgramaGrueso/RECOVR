import { Component, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-splash-screen',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './splash-screen.component.html',
  styleUrls: ['./splash-screen.component.scss']
})
export class SplashScreenComponent implements OnInit, AfterViewInit {
  showSplash = false;
  isFadingOut = false;

  @ViewChild('splashVideo') videoRef?: ElementRef<HTMLVideoElement>;

  ngOnInit(): void {
    const hasVisited = sessionStorage.getItem('recovr_visited');
    if (!hasVisited) {
      this.showSplash = true;
      sessionStorage.setItem('recovr_visited', 'true');
    }
  }

  ngAfterViewInit(): void {
    if (this.showSplash && this.videoRef) {
      const video = this.videoRef.nativeElement;
      
      const tryPlay = () => {
        const playPromise = video.play();
        if (playPromise !== undefined) {
          playPromise.catch(() => {
            // Autoplay might have been blocked
            this.finishSplash();
          });
        }
      };

      if (video.readyState >= 3) {
        tryPlay();
      } else {
        video.addEventListener('loadeddata', tryPlay);
        video.addEventListener('canplay', tryPlay);
      }

      // Fallback if video takes too long or fails to emit ended event
      setTimeout(() => {
        this.finishSplash();
      }, 7000); // Max 7 seconds duration
    }
  }

  onVideoEnded(): void {
    this.finishSplash();
  }

  private finishSplash(): void {
    if (!this.showSplash || this.isFadingOut) return;
    this.isFadingOut = true;
    setTimeout(() => {
      this.showSplash = false;
    }, 600); // 600ms para coincidir con la transición de CSS
  }
}
