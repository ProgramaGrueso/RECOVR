import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from '../../components/hero/hero.component';
import { ScrollShowcaseComponent } from '../../components/scroll-showcase/scroll-showcase.component';
import { ServiceCardComponent } from '../../components/service-card/service-card.component';
import { SectionDividerComponent } from '../../components/section-divider/section-divider.component';
import { CatalogService, VideoReel, ReviewItem } from '../../services/catalog.service';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';
import { ServiceItem } from '../../models/service.model';
import { Professional } from '../../models/professional.model';
import { SpaceItem } from '../../models/space.model';
import { CardTiltDirective } from '../../directives/card-tilt.directive';
import { InViewVideoDirective } from '../../directives/in-view-video.directive';

@Component({
  selector: 'app-home',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    HeroComponent,
    ScrollShowcaseComponent,
    SectionDividerComponent,
    CardTiltDirective,
    InViewVideoDirective
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  private catalogService = inject(CatalogService);
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);
  private cdr = inject(ChangeDetectorRef);

  featuredServices: ServiceItem[] = [];
  filteredServices: ServiceItem[] = [];
  professionals: Professional[] = [];
  spaces: SpaceItem[] = [];
  activeSpace = 0;
  videoReels: VideoReel[] = [];
  reviews: ReviewItem[] = [];
  activeCategory: string = 'ALL';

  activeReel: VideoReel | null = null;
  showVideoModal: boolean = false;

  cinemaService: ServiceItem | null = null;
  showCinemaModal: boolean = false;

  setActiveSpace(index: number): void {
    if (this.activeSpace !== index) {
      this.activeSpace = index;
      this.cdr.markForCheck();
    }
  }

  /** En pantallas táctiles el primer toque abre la cabina; el segundo reserva. */
  onSpaceClick(index: number): void {
    if (this.activeSpace === index) {
      this.onReserve();
    } else {
      this.setActiveSpace(index);
    }
  }

  ngOnInit(): void {
    this.catalogService.getFeaturedServices().subscribe(services => {
      this.featuredServices = services;
      this.filteredServices = services;
      this.cdr.markForCheck();
    });

    this.catalogService.getProfessionals().subscribe(profs => {
      this.professionals = profs;
      this.cdr.markForCheck();
    });

    this.catalogService.getSpaces().subscribe(spaces => {
      this.spaces = spaces;
      this.cdr.markForCheck();
    });

    this.catalogService.getVideoReels().subscribe(reels => {
      this.videoReels = reels;
      this.cdr.markForCheck();
    });

    this.catalogService.getReviews().subscribe(revs => {
      this.reviews = revs;
      this.cdr.markForCheck();
    });
  }

  filterCategory(category: string): void {
    this.activeCategory = category;
    if (category === 'ALL') {
      this.filteredServices = this.featuredServices;
    } else {
      this.filteredServices = this.featuredServices.filter(s =>
        s.category && s.category.toUpperCase().includes(category.toUpperCase())
      );
    }
    this.cdr.markForCheck();
  }

  getSpanClass(index: number): string {
    const spans = ['span-7 tall', 'span-5', 'span-6', 'span-6', 'span-5', 'span-7 tall'];
    return spans[index % spans.length];
  }

  openVideoModal(reel: VideoReel): void {
    this.activeReel = reel;
    this.showVideoModal = true;
    this.cdr.markForCheck();
  }

  closeVideoModal(): void {
    this.showVideoModal = false;
    this.activeReel = null;
    this.cdr.markForCheck();
  }

  openCinemaModal(service: ServiceItem): void {
    this.cinemaService = service;
    this.showCinemaModal = true;
    this.cdr.markForCheck();
  }

  closeCinemaModal(): void {
    this.showCinemaModal = false;
    this.cinemaService = null;
    this.cdr.markForCheck();
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
      video.muted = true;
      video.play().catch(() => {});
    }
  }

  onReelLeave(video: HTMLVideoElement): void {
    if (video) {
      video.pause();
    }
  }

  onCardVideoHover(video: HTMLVideoElement): void {
    if (video) {
      video.muted = true;
      video.currentTime = 0;
      video.play().catch(() => {});
    }
  }

  onCardVideoLeave(video: HTMLVideoElement): void {
    if (video) {
      video.pause();
    }
  }

  onStaffVideoHover(video: HTMLVideoElement): void {
    if (video) {
      video.muted = true;
      video.play().catch(() => {});
    }
  }

  onStaffVideoLeave(video: HTMLVideoElement): void {
    if (video) {
      video.pause();
    }
  }
}

