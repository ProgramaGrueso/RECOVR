import { ChangeDetectorRef, Component, OnInit, OnDestroy, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { BookingService } from '../../services/booking.service';
import { CatalogService } from '../../services/catalog.service';
import { AuthService } from '../../services/auth.service';
import { ApiService, ApiBusinessError } from '../../services/api.service';
import { ServiceItem } from '../../models/service.model';
import { Professional } from '../../models/professional.model';
import { BookingResponse, TurnoDisponible } from '../../models/booking.model';
import { ServerTimeService } from '../../services/server-time.service';

import { BrandMarkComponent } from '../brand-mark/brand-mark.component';

@Component({
  selector: 'app-booking-flow',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, BrandMarkComponent],
  templateUrl: './booking-flow.component.html',
  styleUrls: ['./booking-flow.component.scss']
})
export class BookingFlowComponent implements OnInit, OnDestroy {

  @Input() set preselectedServiceId(id: string | null) {
    if (id) {
      this.handlePreselectedServiceId(id);
    }
  }

  auth = inject(AuthService);
  private router = inject(Router);
  private serverTimeService = inject(ServerTimeService);
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  private step = 1;
  /** Al entrar al paso de fecha y turno se consulta la disponibilidad real de la terapeuta elegida. */
  get currentStep(): number { return this.step; }
  set currentStep(value: number) {
    const previo = this.step;
    this.step = value;
    if (value === 3 && previo !== 3) {
      this.refreshSlots();
    }
  }
  isSubmitting = false;
  isLoadingServices = true;
  isLoadingProfessionals = true;
  hasError = false;
  errorMessage = '';
  bookingErrorTitle = 'SISTEMA TEMPORALMENTE NO DISPONIBLE';
  bookingSuccess: BookingResponse | null = null;

  // Modal Gatekeeper de Autenticación Requerida
  showAuthGateModal = false;
  authModalTab: 'LOGIN' | 'REGISTER' = 'LOGIN';
  authModalEmail = '';
  authModalPassword = '';
  authModalName = '';
  authModalPhone = '';
  authModalError = '';
  authModalSuccess = '';

  services: ServiceItem[] = [];
  professionals: Professional[] = [];

  bookingForm!: FormGroup;
  private subs = new Subscription();

  /** Turnos del día con su disponibilidad real (el servidor explica por qué un turno no se puede tomar). */
  slots: TurnoDisponible[] = [];
  isLoadingSlots = false;
  slotsError = '';

  // Pago: solo el pago completo asegura el turno. Mientras tanto la reserva queda apartada un tiempo limitado.
  readonly holdMinutes = 15;
  readonly payMethods = ['YAPE', 'PLIN', 'TARJETA', 'TRANSFERENCIA'];
  payMethod = 'YAPE';
  isPaying = false;
  payError = '';
  secondsLeft = 0;
  private payTimer: ReturnType<typeof setInterval> | null = null;

  constructor(
    private bookingService: BookingService,
    private catalogService: CatalogService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    if (!this.pendingServiceId) {
      this.bookingService.clearSelectedService();
      this.currentStep = 1;
    }
    this.loadInitialData();

    this.subs.add(
      this.bookingService.selectedService$.subscribe(service => {
        if (service) {
          this.bookingForm.patchValue({ serviceId: service.id });
          if (this.currentStep === 1) {
            this.currentStep = 2;
          }
        }
      })
    );
  }


  private initForm(): void {
    const todayStr = this.serverTimeService.getNowISOString().split('T')[0];
    this.bookingForm = this.fb.group({
      serviceId: ['', Validators.required],
      professionalId: ['', Validators.required],
      date: [todayStr, Validators.required],
      timeSlot: ['', Validators.required],
      clientName: ['', [Validators.required, Validators.minLength(3)]],
      clientEmail: ['', [Validators.required, Validators.email]],
      clientPhone: ['', [Validators.required, Validators.pattern('^[0-9+\\s-]{8,}$')]],
      notes: ['']
    });
    this.subs.add(this.bookingForm.get('date')!.valueChanges.subscribe(() => {
      this.bookingForm.patchValue({ timeSlot: '' }, { emitEvent: false });
      if (this.currentStep === 3) {
        this.refreshSlots();
      }
    }));
  }

  /** Consulta al servidor qué turnos de la fecha elegida se pueden reservar con esa terapeuta y servicio. */
  refreshSlots(): void {
    const srv = this.selectedService;
    const pro = this.selectedProfessional;
    const date = this.bookingForm.get('date')?.value;
    if (!srv || !pro || !date) {
      return;
    }
    this.isLoadingSlots = true;
    this.slotsError = '';
    this.subs.add(this.bookingService.availability(srv.code, pro.name, date).subscribe({
      next: slots => {
        this.slots = slots;
        const elegido = this.bookingForm.get('timeSlot')?.value;
        if (elegido && !slots.some(s => s.hora === elegido && s.disponible)) {
          this.bookingForm.patchValue({ timeSlot: '' });
        }
        this.isLoadingSlots = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.slots = [];
        this.isLoadingSlots = false;
        this.slotsError = err instanceof ApiBusinessError ? err.message : 'No se pudo consultar la disponibilidad. Intenta de nuevo.';
        this.cdr.markForCheck();
      }
    }));
  }

  loadInitialData(): void {
    this.isLoadingServices = true;
    this.isLoadingProfessionals = true;
    this.hasError = false;

    this.subs.add(
      this.catalogService.getFeaturedServices().subscribe({
        next: (srvs) => {
          this.services = srvs;
          this.isLoadingServices = false;
          this.checkAndApplyPendingSelection();
        },
        error: (err) => {
          this.isLoadingServices = false;
          this.hasError = true;
          this.errorMessage = 'No se pudo conectar con el catálogo de protocolos.';
        }
      })
    );

    this.subs.add(
      this.catalogService.getProfessionals().subscribe({
        next: (profs) => {
          this.professionals = profs;
          this.isLoadingProfessionals = false;
        },
        error: (err) => {
          this.isLoadingProfessionals = false;
          this.hasError = true;
          this.errorMessage = 'No se pudo cargar el personal de kinesiólogos y podólogos.';
        }
      })
    );
  }

  private pendingServiceId?: string;

  private handlePreselectedServiceId(id: string): void {
    this.pendingServiceId = id;
    this.checkAndApplyPendingSelection();
  }

  private checkAndApplyPendingSelection(): void {
    if (!this.pendingServiceId || this.services.length === 0) return;
    
    const matched = this.services.find(
      s => s.id === this.pendingServiceId || s.name.toLowerCase().includes(this.pendingServiceId!.toLowerCase())
    );

    if (matched) {
      this.bookingForm.patchValue({ serviceId: matched.id });
      this.currentStep = 2;
    }
  }

  get selectedService(): ServiceItem | undefined {
    const id = this.bookingForm.get('serviceId')?.value;
    return this.services.find(s => s.id === id);
  }

  get selectedProfessional(): Professional | undefined {
    const id = this.bookingForm.get('professionalId')?.value;
    return this.professionals.find(p => p.id === id);
  }

  get filteredProfessionals(): Professional[] {
    const srv = this.selectedService;
    if (!srv) return this.professionals;
    // Las terapeutas de categoría MASAJE atienden todas las líneas del catálogo (tántrico, nuru, 4 manos, VIP...).
    return this.professionals.filter(p => p.category === srv.category || p.category === 'MASAJE');
  }

  selectService(service: ServiceItem): void {
    this.bookingForm.patchValue({ serviceId: service.id });
    this.currentStep = 2;
  }

  selectProfessional(professional: Professional): void {
    this.bookingForm.patchValue({ professionalId: professional.id });
    this.currentStep = 3;
  }

  selectTimeSlot(slot: TurnoDisponible): void {
    if (!slot.disponible) {
      return;
    }
    this.bookingForm.patchValue({ timeSlot: slot.hora });
  }

  get isClientLoggedIn(): boolean {
    return this.auth.isClient();
  }

  private fillClientDataIfLoggedIn(): void {
    if (this.isClientLoggedIn) {
      const u = this.auth.user();
      if (u) {
        this.bookingForm.patchValue({ clientName: u.nombre, clientEmail: u.correo, clientPhone: u.telefono ?? '' });
      }
    }
  }

  goToStep(step: number): void {
    if (step === 4 && !this.isClientLoggedIn) {
      this.openAuthGate('LOGIN');
      return;
    }
    this.currentStep = step;
    if (step === 4) {
      this.fillClientDataIfLoggedIn();
    }
  }

  nextStep(): void {
    if (this.currentStep === 3) {
      if (!this.isClientLoggedIn) {
        this.openAuthGate('LOGIN');
        return;
      }
      this.fillClientDataIfLoggedIn();
      this.currentStep = 4;
      return;
    }

    if (this.currentStep < 4) {
      this.currentStep++;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  openAuthGate(tab: 'LOGIN' | 'REGISTER' = 'LOGIN'): void {
    this.authModalTab = tab;
    this.authModalError = '';
    this.authModalSuccess = '';
    this.showAuthGateModal = true;
  }

  closeAuthGate(): void {
    this.showAuthGateModal = false;
    this.authModalError = '';
    this.authModalSuccess = '';
  }

  submitAuthModalLogin(): void {
    this.authModalError = '';
    if (!this.authModalEmail || !this.authModalPassword) {
      this.authModalError = 'Ingresa tu correo y contraseña.';
      return;
    }
    this.auth.login(this.authModalEmail.trim(), this.authModalPassword).subscribe(res => {
      this.authModalPassword = '';
      if (!res.ok) {
        this.authModalError = res.message ?? 'No se pudo iniciar sesión.';
      } else if (!this.auth.isClient()) {
        this.auth.logout();
        this.authModalError = 'Esta cuenta es del personal. Inicia sesión como cliente para agendar.';
      } else {
        this.fillClientDataIfLoggedIn();
        this.showAuthGateModal = false;
        this.currentStep = 4;
      }
      this.cdr.markForCheck();
    });
  }

  submitAuthModalRegister(): void {
    this.authModalError = '';
    this.auth.registrar(this.authModalName.trim(), this.authModalEmail.trim(), this.authModalPassword, this.authModalPhone.trim()).subscribe(res => {
      this.authModalPassword = '';
      if (res.ok) {
        this.fillClientDataIfLoggedIn();
        this.showAuthGateModal = false;
        this.currentStep = 4;
      } else {
        this.authModalError = res.message ?? 'No se pudo crear la cuenta.';
      }
      this.cdr.markForCheck();
    });
  }

  onSubmit(): void {
    if (!this.isClientLoggedIn) {
      this.openAuthGate('LOGIN');
      return;
    }

    if (this.bookingForm.invalid) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.hasError = false;
    this.bookingService.submitBooking(this.bookingForm.value, this.selectedService?.code, this.selectedProfessional?.name).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.bookingSuccess = response;
        this.payError = '';
        if (response.status === 'PENDIENTE') {
          this.startCountdown();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.hasError = true;
        this.bookingErrorTitle = err instanceof ApiBusinessError ? 'NO SE PUDO RESERVAR ESE TURNO' : 'SISTEMA TEMPORALMENTE NO DISPONIBLE';
        this.errorMessage = err instanceof ApiBusinessError
          ? err.message
          : 'Ocurrió un fallo al confirmar la reserva. Intenta nuevamente.';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  /** Cuenta regresiva del plazo para pagar; al llegar a cero el servidor libera el cupo. */
  private startCountdown(): void {
    this.stopCountdown();
    const limite = Date.now() + this.holdMinutes * 60_000;
    const tick = () => {
      this.secondsLeft = Math.max(0, Math.round((limite - Date.now()) / 1000));
      if (this.secondsLeft === 0) {
        this.stopCountdown();
      }
      this.cdr.markForCheck();
    };
    tick();
    this.payTimer = setInterval(tick, 1000);
  }

  private stopCountdown(): void {
    if (this.payTimer) {
      clearInterval(this.payTimer);
      this.payTimer = null;
    }
  }

  get countdownText(): string {
    const m = Math.floor(this.secondsLeft / 60);
    const s = this.secondsLeft % 60;
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  get hasAvailableSlots(): boolean {
    return this.slots.some(s => s.disponible);
  }

  get holdExpired(): boolean {
    return this.bookingSuccess?.status === 'PENDIENTE' && this.secondsLeft === 0;
  }

  /** Paga el importe completo que fija el servidor; solo así la reserva queda CONFIRMADA y el turno asegurado. */
  payNow(): void {
    const b = this.bookingSuccess;
    if (!b || b.reservaId == null || b.price == null || this.isPaying || this.holdExpired) {
      return;
    }
    this.isPaying = true;
    this.payError = '';
    this.subs.add(this.bookingService.payBooking(b.reservaId, b.price, this.payMethod).subscribe({
      next: paid => {
        this.isPaying = false;
        this.stopCountdown();
        this.bookingSuccess = { ...b, status: paid.status };
        this.cdr.markForCheck();
      },
      error: err => {
        this.isPaying = false;
        this.payError = err instanceof ApiBusinessError ? err.message : 'No se pudo registrar el pago. Intenta de nuevo.';
        this.cdr.markForCheck();
      }
    }));
  }

  resetFlow(): void {
    this.stopCountdown();
    this.bookingSuccess = null;
    this.currentStep = 1;
    this.bookingForm.reset();
  }

  ngOnDestroy(): void {
    this.stopCountdown();
    this.subs.unsubscribe();
  }
}

