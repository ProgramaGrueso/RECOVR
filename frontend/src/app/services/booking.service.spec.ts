import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ApiBusinessError, ApiService } from './api.service';
import { BookingService } from './booking.service';
import { BookingRequest } from '../models/booking.model';

const REQUEST: BookingRequest = {
  serviceId: 'rc-01', professionalId: 'prof-01', date: '2026-11-18', timeSlot: '17:30',
  clientName: 'Ana', clientEmail: 'ana@recovr.test', clientPhone: '999111222'
};

describe('BookingService', () => {
  let api: { hasSession: boolean; reservar: ReturnType<typeof vi.fn> };
  let service: BookingService;

  function crear(hasSession: boolean) {
    const store = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (k: string) => store.get(k) ?? null, setItem: (k: string, v: string) => void store.set(k, v),
      removeItem: (k: string) => void store.delete(k), clear: () => store.clear()
    });
    vi.stubGlobal('sessionStorage', localStorage);
    api = { hasSession, reservar: vi.fn() };
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: ApiService, useValue: api }] });
    service = TestBed.inject(BookingService);
  }

  afterEach(() => vi.unstubAllGlobals());

  it('sin sesión del backend usa la simulación local', async () => {
    crear(false);
    const res = await new Promise<{ status: string }>(r => service.submitBooking(REQUEST, 'RC-01', 'Ely').subscribe(r));
    expect(api.reservar).not.toHaveBeenCalled();
    expect(res.status).toBe('CONFIRMADA');
  });

  it('con sesión crea la reserva real y queda PENDIENTE', async () => {
    crear(true);
    api.reservar.mockReturnValue(of({ id: 'REC-000001', status: 'PENDIENTE', createdAt: '', serviceName: 'RC-01', professionalName: 'Ely', date: '2026-11-18', timeSlot: '17:30' }));
    const res = await new Promise<{ id: string; status: string }>(r => service.submitBooking(REQUEST, 'RC-01', 'Ely').subscribe(r));
    expect(api.reservar).toHaveBeenCalledWith(REQUEST, 'RC-01', 'Ely');
    expect(res).toMatchObject({ id: 'REC-000001', status: 'PENDIENTE' });
  });

  it('un conflicto de agenda se propaga y no se simula', async () => {
    crear(true);
    api.reservar.mockReturnValue(throwError(() => new ApiBusinessError('Turno ocupado')));
    const error = await new Promise<unknown>(r => service.submitBooking(REQUEST, 'RC-01', 'Ely').subscribe({ error: r }));
    expect(error).toBeInstanceOf(ApiBusinessError);
  });

  it('si el backend cae (error de red) vuelve a la simulación local', async () => {
    crear(true);
    api.reservar.mockReturnValue(throwError(() => new Error('network')));
    const res = await new Promise<{ status: string }>(r => service.submitBooking(REQUEST, 'RC-01', 'Ely').subscribe(r));
    expect(res.status).toBe('CONFIRMADA');
  });
});
