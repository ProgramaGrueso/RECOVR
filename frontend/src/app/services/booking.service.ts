import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { BookingRequest, BookingResponse } from '../models/booking.model';
import { ServiceItem } from '../models/service.model';
import { ApiService, ApiBusinessError } from './api.service';
import { AuthService } from './auth.service';
import { ServerTimeService } from './server-time.service';

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private selectedServiceSubject = new BehaviorSubject<ServiceItem | null>(null);
  selectedService$ = this.selectedServiceSubject.asObservable();

  private router = inject(Router);
  private auth = inject(AuthService);
  private api = inject(ApiService);
  private serverTimeService = inject(ServerTimeService);

  constructor() {}

  clearSelectedService(): void {
    this.selectedServiceSubject.next(null);
  }

  navigateToBooking(service?: ServiceItem): void {
    if (!this.auth.isClient()) {
      alert('Debes crear una cuenta o iniciar sesión para poder reservar.');
      this.router.navigate(['/admin/login']);
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
   * Con sesión en el backend crea la reserva real (queda PENDIENTE hasta que se confirme y pague).
   * Un conflicto de agenda se informa al usuario; si el backend no responde se usa la simulación local
   * para que la demo publicada siga funcionando.
   */
  submitBooking(request: BookingRequest, serviceCode?: string, professionalName?: string): Observable<BookingResponse> {
    if (this.api.hasSession && serviceCode && professionalName) {
      return this.api.reservar(request, serviceCode, professionalName).pipe(
        catchError(err => err instanceof ApiBusinessError ? throwError(() => err) : this.simulatedBooking(request))
      );
    }
    return this.simulatedBooking(request);
  }

  private simulatedBooking(request: BookingRequest): Observable<BookingResponse> {
    const mockResponse: BookingResponse = {
      id: 'REC-' + Math.floor(100000 + Math.random() * 900000),
      status: 'CONFIRMADA',
      createdAt: this.serverTimeService.getNowISOString(),
      serviceName: 'Protocolo de Recuperación',
      professionalName: 'Especialista RECOVR',
      date: request.date,
      timeSlot: request.timeSlot
    };
    return of(mockResponse).pipe(delay(800));
  }
}
