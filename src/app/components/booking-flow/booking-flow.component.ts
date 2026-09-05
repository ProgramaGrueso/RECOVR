import { Component, OnInit, OnDestroy, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { BookingService } from '../../services/booking.service';
import { CatalogService } from '../../services/catalog.service';
import { AuthService } from '../../services/auth.service';
import { ServiceItem } from '../../models/service.model';
import { Professional } from '../../models/professional.model';
import { BookingResponse } from '../../models/booking.model';

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

  currentStep = 1;
  isSubmitting = false;
  isLoadingServices = true;
  isLoadingProfessionals = true;
  hasError = false;
  errorMessage = '';
  bookingSuccess: BookingResponse | null = null;
  earnedPoints = 0;
  totalClientPoints = 0;

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

  timeSlots = [
    '09:00', '10:30', '12:00', '14:30', '16:00', '17:30', '19:00'
  ];

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
    const todayStr = new Date().toISOString().split('T')[0];
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
    return this.professionals.filter(p => p.category === srv.category);
  }

  selectService(service: ServiceItem): void {
    this.bookingForm.patchValue({ serviceId: service.id });
    this.currentStep = 2;
  }

  selectProfessional(professional: Professional): void {
    this.bookingForm.patchValue({ professionalId: professional.id });
    this.currentStep = 3;
  }

  selectTimeSlot(slot: string): void {
    this.bookingForm.patchValue({ timeSlot: slot });
  }

  get isClientLoggedIn(): boolean {
    return this.auth.isAuthenticated() && this.auth.currentUser()?.role === 'CLIENTE';
  }

  private fillClientDataIfLoggedIn(): void {
    if (this.isClientLoggedIn) {
      const u = this.auth.currentUser();
      if (u) {
        this.bookingForm.patchValue({
          clientName: u.name,
          clientEmail: u.email,
          clientPhone: u.phone || '+56 9 8765 4321'
        });
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

  quickLoginDemoClient(): void {
    this.auth.loginAsRole('CLIENTE');
    this.fillClientDataIfLoggedIn();
    this.showAuthGateModal = false;
    this.currentStep = 4;
  }

  submitAuthModalLogin(): void {
    this.authModalError = '';
    if (!this.authModalEmail || !this.authModalPassword) {
      this.authModalError = 'Ingresa tu correo/usuario y contraseña.';
      return;
    }
    const success = this.auth.login(this.authModalEmail, this.authModalPassword);
    if (success && this.auth.currentUser()?.role === 'CLIENTE') {
      this.fillClientDataIfLoggedIn();
      this.showAuthGateModal = false;
      this.currentStep = 4;
    } else if (success && this.auth.currentUser()?.role !== 'CLIENTE') {
      this.authModalError = 'Esta sesión es del personal. Inicia sesión como cliente para agendar.';
    } else {
      this.authModalError = 'Credenciales inválidas. Verifica tu correo y contraseña.';
    }
  }

  submitAuthModalRegister(): void {
    this.authModalError = '';
    const res = this.auth.registerClient({
      name: this.authModalName,
      email: this.authModalEmail,
      phone: this.authModalPhone,
      password: this.authModalPassword
    });

    if (res.success) {
      this.fillClientDataIfLoggedIn();
      this.showAuthGateModal = false;
      this.currentStep = 4;
    } else {
      this.authModalError = res.message;
    }
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
    this.bookingService.submitBooking(this.bookingForm.value).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.bookingSuccess = response;
        this.earnedPoints = 100;
        this.totalClientPoints = this.auth.addClientPoints(100, `Reserva confirmada: ${this.selectedService?.name || 'Protocolo'}`);
        this.auth.addClientBooking({
          id: response.id,
          service: this.selectedService?.name || 'Protocolo Sanctum',
          prof: this.selectedProfessional?.name || 'Especialista RECOVR',
          room: 'Sanctum 01 (Descompresión)',
          date: response.date,
          time: response.timeSlot,
          price: this.selectedService?.price || 65000,
          status: 'CONFIRMADA',
          notes: this.bookingForm.value.notes
        });
      },
      error: (err) => {
        this.isSubmitting = false;
        this.hasError = true;
        this.errorMessage = 'Ocurrió un fallo al confirmar la reserva. Intenta nuevamente.';
      }
    });
  }

  resetFlow(): void {
    this.bookingSuccess = null;
    this.currentStep = 1;
    this.bookingForm.reset();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }
}

