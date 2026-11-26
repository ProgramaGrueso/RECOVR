import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, HostListener, ViewChild, ElementRef, inject } from '@angular/core';
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
    InViewVideoDirective
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  private catalogService = inject(CatalogService);
  private bookingService = inject(BookingService);
  private cursorService = inject(CursorService);
  private cdr = inject(ChangeDetectorRef);

  featuredServices: ServiceItem[] = [];
  filteredServices: ServiceItem[] = [];
  activeCategory = 'ALL';
  professionals: Professional[] = [];
  spaces: SpaceItem[] = [];
  activeSpace = 0;
  videoReels: VideoReel[] = [];
  reviews: ReviewItem[] = [];

  /* Carta cinema (coverflow) */
  cinemaIndex = 0;
  blancoNegro = false;
  hora = '';
  private reloj?: ReturnType<typeof setInterval>;

  readonly filtros = ['ALL', 'TÁNTRICO', 'NURU', 'DESCONTRACTURANTE', '4 MANOS', 'VIP'];

  @ViewChild('cineSeccion') cineSeccion?: ElementRef<HTMLElement>;

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
    this.actualizarHora();
    this.reloj = setInterval(() => this.actualizarHora(), 1000);

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

  ngOnDestroy(): void {
    if (this.reloj) clearInterval(this.reloj);
  }

  private actualizarHora(): void {
    this.hora = new Date().toLocaleTimeString('es-PE', {
      timeZone: 'America/Lima',
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
    this.cdr.markForCheck();
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
    this.cinemaIndex = 0;
    this.cdr.markForCheck();
  }

  /* ---------------------------------------------------------- carta cinema (coverflow) */

  get servicioActivo(): ServiceItem | null {
    return this.filteredServices[this.cinemaIndex] ?? null;
  }

  cuenta(categoria: string): number {
    if (categoria === 'ALL') return this.featuredServices.length;
    return this.featuredServices.filter(s => (s.category || '').toUpperCase().includes(categoria)).length;
  }

  mover(delta: number): void {
    const total = this.filteredServices.length;
    if (!total) return;
    this.cinemaIndex = (this.cinemaIndex + delta + total) % total;
    this.cdr.markForCheck();
  }

  irA(indice: number): void {
    this.cinemaIndex = indice;
    this.cdr.markForCheck();
  }

  seleccionar(indice: number): void {
    if (indice === this.cinemaIndex) {
      const servicio = this.filteredServices[indice];
      if (servicio) this.openCinemaModal(servicio);
      return;
    }
    this.irA(indice);
  }

  transformar(indice: number): string {
    const d = indice - this.cinemaIndex;
    const abs = Math.abs(d);
    if (abs > 2) return 'translateX(0) translateZ(-900px) scale(0.6)';
    const x = d * 242;
    const z = -abs * 120;
    const ry = d === 0 ? 0 : d > 0 ? -30 : 30;
    const escala = 1 - abs * 0.08;
    return `translateX(${x}px) translateZ(${z}px) rotateY(${ry}deg) scale(${escala})`;
  }

  opacidad(indice: number): number {
    const abs = Math.abs(indice - this.cinemaIndex);
    return abs > 2 ? 0 : Math.max(0.25, 1 - abs * 0.22);
  }

  capa(indice: number): number {
    return 20 - Math.abs(indice - this.cinemaIndex);
  }

  alternarBn(): void {
    this.blancoNegro = !this.blancoNegro;
    this.cdr.markForCheck();
  }

  onCinemaKey(evento: KeyboardEvent): void {
    if (evento.key === 'ArrowLeft') {
      evento.preventDefault();
      this.mover(-1);
    } else if (evento.key === 'ArrowRight') {
      evento.preventDefault();
      this.mover(1);
    } else if (evento.key === 'Enter') {
      const servicio = this.servicioActivo;
      if (servicio) this.openCinemaModal(servicio);
    }
  }

  /** Flechas/Enter para la carta cinema, solo cuando está a la vista y no se escribe en un campo. */
  @HostListener('window:keydown', ['$event'])
  onTecladoGlobal(evento: KeyboardEvent): void {
    const destino = evento.target as HTMLElement | null;
    if (destino && /^(INPUT|TEXTAREA|SELECT)$/.test(destino.tagName)) return;
    if (this.showCinemaModal || this.showVideoModal) return;

    const seccion = this.cineSeccion?.nativeElement;
    if (!seccion) return;
    const caja = seccion.getBoundingClientRect();
    const visible = caja.top < window.innerHeight * 0.75 && caja.bottom > window.innerHeight * 0.25;
    if (!visible) return;

    this.onCinemaKey(evento);
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

