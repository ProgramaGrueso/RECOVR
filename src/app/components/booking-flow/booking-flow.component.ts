import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { BookingService } from '../../services/booking.service';
import { CatalogService } from '../../services/catalog.service';
import { ServiceItem } from '../../models/service.model';
import { Professional } from '../../models/professional.model';
import { BookingResponse } from '../../models/booking.model';

import { BrandMarkComponent } from '../brand-mark/brand-mark.component';

@Component({
  selector: 'app-booking-flow',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, BrandMarkComponent],
  templateUrl: './booking-flow.component.html',
  styleUrls: ['./booking-flow.component.scss']
})
export class BookingFlowComponent implements OnInit, OnDestroy {

  @Input() set preselectedServiceId(id: string | null) {
    if (id) {
      this.handlePreselectedServiceId(id);
    }
  }

  currentStep = 1;
  isSubmitting = false;
  isLoadingServices = true;
  isLoadingProfessionals = true;
  hasError = false;
  errorMessage = '';
  bookingSuccess: BookingResponse | null = null;

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

  goToStep(step: number): void {
    this.currentStep = step;
  }

  nextStep(): void {
    if (this.currentStep < 4) {
      this.currentStep++;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  onSubmit(): void {
    if (this.bookingForm.invalid) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.bookingService.submitBooking(this.bookingForm.value).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.bookingSuccess = response;
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

