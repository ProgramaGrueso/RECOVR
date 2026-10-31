import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, inject } from '@angular/core';
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
import { FiltroTratamiento, TratamientosCarruselComponent } from '../../components/tratamientos-carrusel/tratamientos-carrusel.component';
import { AccionesPaginaService } from '../../services/acciones-pagina.service';

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
    InViewVideoDirective,
    TratamientosCarruselComponent
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
  readonly filtrosTratamientos: FiltroTratamiento[] = [
    { key: 'ALL', label: 'TODOS' },
    { key: 'TÁNTRICO', label: 'TÁNTRICO' },
    { key: 'NURU', label: 'NURU BODY SLIDE' },
    { key: 'DESCONTRACTURANTE', label: 'DESCONTRACTURANTE' },
    { key: '4 MANOS', label: '4 MANOS' },
    { key: 'VIP', label: 'VIP HIDROMASAJE' }
  ];
  professionals: Professional[] = [];
  spaces: SpaceItem[] = [];
  activeSpace = 0;
  videoReels: VideoReel[] = [];
  reviews: ReviewItem[] = [];

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

  constructor() {
    // La asistente puede abrir un tratamiento (su video) desde el chat
    const quitar = inject(AccionesPaginaService).registrarPortada(s => this.openCinemaModal(s));
    inject(DestroyRef).onDestroy(quitar);
  }

  ngOnInit(): void {
    this.catalogService.getFeaturedServices().subscribe(services => {
      this.featuredServices = services;
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

