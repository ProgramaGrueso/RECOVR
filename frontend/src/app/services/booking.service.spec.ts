import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Router } from '@angular/router';
import { of, throwError, firstValueFrom } from 'rxjs';
import { BookingService } from './booking.service';
import { ApiBusinessError, ApiService } from './api.service';
import { AuthService } from './auth.service';
import { BookingRequest } from '../models/booking.model';

const REQUEST: BookingRequest = {
  serviceId: 'rc-01', professionalId: 'prof-01', date: '2026-11-18', timeSlot: '17:30',
  clientName: 'Ana', clientEmail: 'ana@recovr.test', clientPhone: '999111222'
};

describe('BookingService', () => {
  let service: BookingService;
  let api: { hasSession: boolean; reservar: ReturnType<typeof vi.fn> };

  function crear(hasSession: boolean, esCliente = true): void {
    api = { hasSession, reservar: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: ApiService, useValue: api },
        { provide: AuthService, useValue: { isClient: () => esCliente } },
        { provide: Router, useValue: { navigate: vi.fn().mockResolvedValue(true) } }
      ]
    });
    service = TestBed.inject(BookingService);
  }

  it('sin sesión NO simula la reserva: informa que hay que iniciar sesión', async () => {
    crear(false);
    await expect(firstValueFrom(service.submitBooking(REQUEST, 'RC-01', 'Ely'))).rejects.toBeInstanceOf(ApiBusinessError);
    expect(api.reservar).not.toHaveBeenCalled();
  });

  it('con sesión crea la reserva real y queda PENDIENTE', async () => {
    crear(true);
    api.reservar.mockReturnValue(of({ id: 'REC-000001', status: 'PENDIENTE', createdAt: '', serviceName: 'RC-01', professionalName: 'Ely', date: '2026-11-18', timeSlot: '17:30' }));
    const r = await firstValueFrom(service.submitBooking(REQUEST, 'RC-01', 'Ely'));
    expect(api.reservar).toHaveBeenCalledWith(REQUEST, 'RC-01', 'Ely');
    expect(r.status).toBe('PENDIENTE');
  });

  it('un conflicto de agenda se propaga tal cual', async () => {
    crear(true);
    api.reservar.mockReturnValue(throwError(() => new ApiBusinessError('Turno ocupado')));
    await expect(firstValueFrom(service.submitBooking(REQUEST, 'RC-01', 'Ely'))).rejects.toThrow('Turno ocupado');
  });

  it('si el backend cae, el error llega a la pantalla y no se inventa una reserva', async () => {
    crear(true);
    api.reservar.mockReturnValue(throwError(() => new Error('network')));
    await expect(firstValueFrom(service.submitBooking(REQUEST, 'RC-01', 'Ely'))).rejects.toThrow('network');
  });
});
