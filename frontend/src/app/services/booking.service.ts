import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { BookingRequest, BookingResponse, TurnoDisponible } from '../models/booking.model';
import { ServiceItem } from '../models/service.model';
import { ApiService, ApiBusinessError } from './api.service';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private selectedServiceSubject = new BehaviorSubject<ServiceItem | null>(null);
  selectedService$ = this.selectedServiceSubject.asObservable();

  private router = inject(Router);
  private auth = inject(AuthService);
  private api = inject(ApiService);

  constructor() {}

  clearSelectedService(): void {
    this.selectedServiceSubject.next(null);
  }

  navigateToBooking(service?: ServiceItem): void {
    if (!this.auth.isClient()) {
      alert('Para reservar inicia sesión con tu cuenta de cliente (o crea una).');
      void this.router.navigate(['/admin/login'], { queryParams: { returnUrl: '/reserva' } });
      return;
    }

    if (service) {
      this.selectedServiceSubject.next(service);
      this.router.navigate(['/reserva'], { queryParams: { serviceId: service.id } });
    } else {
      this.clearSelectedService();
      this.router.navigate(['/reserva']);
    }
  }


  // Compatibilidad retroactiva por si se invoca openBookingModal
  openBookingModal(service?: ServiceItem): void {
    this.navigateToBooking(service);
  }

  /**
   * Crea la reserva real en el backend (queda PENDIENTE hasta que recepción la cobre y confirme). Si no hay sesión
   * o el servidor no responde se informa el error: nunca se simula una reserva que no existe en la base de datos.
   */
  submitBooking(request: BookingRequest, serviceCode?: string, professionalName?: string): Observable<BookingResponse> {
    if (!this.api.hasSession) {
      return throwError(() => new ApiBusinessError('Tu sesión expiró. Inicia sesión de nuevo para reservar.'));
    }
    if (!serviceCode || !professionalName) {
      return throwError(() => new ApiBusinessError('Elige un servicio y una terapeuta antes de confirmar.'));
    }
    return this.api.reservar(request, serviceCode, professionalName);
  }

  /** Turnos del día con su disponibilidad real (terapeuta ocupada, sin cabinas, fuera de horario...). */
  availability(serviceCode: string, professionalName: string, date: string): Observable<TurnoDisponible[]> {
    return this.api.disponibilidad(serviceCode, professionalName, date);
  }

  /** Paga completa la reserva recién creada: solo el pago total asegura el turno. */
  payBooking(reservaId: number, amount: number, method: string): Observable<BookingResponse> {
    return this.api.pagarReserva(reservaId, amount, method);
  }
}
