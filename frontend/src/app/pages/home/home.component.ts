import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from '../../components/hero/hero.component';
import { ServiceCardComponent } from '../../components/service-card/service-card.component';
import { ProfessionalCardComponent } from '../../components/professional-card/professional-card.component';
import { SectionDividerComponent } from '../../components/section-divider/section-divider.component';
import { SmokeEffectComponent } from '../../components/smoke-effect/smoke-effect.component';
import { CatalogService, VideoReel, ReviewItem } from '../../services/catalog.service';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';
import { ServiceItem } from '../../models/service.model';
import { Professional } from '../../models/professional.model';
import { SpaceItem } from '../../models/space.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    HeroComponent,
    ServiceCardComponent,
    ProfessionalCardComponent,
    SectionDividerComponent,
    SmokeEffectComponent
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  private catalogService = inject(CatalogService);
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);

  featuredServices: ServiceItem[] = [];
  filteredServices: ServiceItem[] = [];
  professionals: Professional[] = [];
  spaces: SpaceItem[] = [];
  videoReels: VideoReel[] = [];
  reviews: ReviewItem[] = [];
  activeCategory: string = 'ALL';

  activeReel: VideoReel | null = null;
  showVideoModal: boolean = false;

  ngOnInit(): void {
    this.catalogService.getFeaturedServices().subscribe(services => {
      this.featuredServices = services;
      this.filteredServices = services;
    });

    this.catalogService.getProfessionals().subscribe(profs => {
      this.professionals = profs;
    });

    this.catalogService.getSpaces().subscribe(spaces => {
      this.spaces = spaces;
    });

    this.catalogService.getVideoReels().subscribe(reels => {
      this.videoReels = reels;
    });

    this.catalogService.getReviews().subscribe(revs => {
      this.reviews = revs;
    });
  }

  filterCategory(category: string): void {
    this.activeCategory = category;
    if (category === 'ALL') {
      this.filteredServices = this.featuredServices;
    } else {
      this.filteredServices = this.featuredServices.filter(s => s.category.toUpperCase() === category.toUpperCase());
    }
  }

  openVideoModal(reel: VideoReel): void {
    this.activeReel = reel;
    this.showVideoModal = true;
  }

  closeVideoModal(): void {
    this.showVideoModal = false;
    this.activeReel = null;
  }

  onReserve(service?: ServiceItem): void {
    this.bookingService.openBookingModal(service);
  }

  onSelectProfessional(professional: Professional): void {
    this.bookingService.openBookingModal();
  }

  setCursor(label: string) {
    this.cursorService.setCursor(label, true, 'drag');
  }

  resetCursor() {
    this.cursorService.resetCursor();
  }

  onReelHover(video: HTMLVideoElement): void {
    if (video) {
      video.muted = false;
      video.volume = 1.0;
      const playPromise = video.play();
      if (playPromise !== undefined) {
        playPromise.catch(() => {
          if (video.muted === false) {
            video.muted = true;
            video.play().catch(() => {});
          }
        });
      }
    }
  }

  onReelLeave(video: HTMLVideoElement): void {
    if (video) {
      video.pause();
    }
  }
}

