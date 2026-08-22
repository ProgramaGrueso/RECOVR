import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { BookingRequest, BookingResponse } from '../models/booking.model';
import { ServiceItem } from '../models/service.model';

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private selectedServiceSubject = new BehaviorSubject<ServiceItem | null>(null);
  selectedService$ = this.selectedServiceSubject.asObservable();

  constructor(private router: Router) {}

  clearSelectedService(): void {
    this.selectedServiceSubject.next(null);
  }

  navigateToBooking(service?: ServiceItem): void {
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

  submitBooking(request: BookingRequest): Observable<BookingResponse> {
    const mockResponse: BookingResponse = {
      id: 'REC-' + Math.floor(100000 + Math.random() * 900000),
      status: 'CONFIRMADA',
      createdAt: new Date().toISOString(),
      serviceName: 'Protocolo de Recuperación',
      professionalName: 'Especialista RECOVR',
      date: request.date,
      timeSlot: request.timeSlot
    };

    // Simulación de delay de API REST Spring Boot
    return of(mockResponse).pipe(delay(800));
  }
}

