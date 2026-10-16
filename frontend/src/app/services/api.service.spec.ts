import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApiBusinessError, ApiService } from './api.service';
import { BookingRequest } from '../models/booking.model';

const API = 'http://localhost:8095/api';
const REQUEST: BookingRequest = {
  serviceId: 'rc-01', professionalId: 'prof-01', date: '2026-11-18', timeSlot: '17:30',
  clientName: 'Ana', clientEmail: 'ana@recovr.test', clientPhone: '999111222'
};

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    const store = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (k: string) => store.get(k) ?? null,
      setItem: (k: string, v: string) => void store.set(k, v),
      removeItem: (k: string) => void store.delete(k),
      clear: () => store.clear()
    });
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.unstubAllGlobals();
  });

  it('guarda el token al iniciar sesión y lo borra al cerrar', () => {
    let ok = false;
    api.login('ana@recovr.test', 'Clave123').subscribe(r => (ok = r));
    http.expectOne(`${API}/auth/login`).flush({ token: 'jwt-123' });
    expect(ok).toBe(true);
    expect(api.hasSession).toBe(true);
    api.clearSession();
    expect(api.hasSession).toBe(false);
  });

  it('devuelve false si el backend no responde al login', () => {
    let ok = true;
    api.login('ana@recovr.test', 'x').subscribe(r => (ok = r));
    http.expectOne(`${API}/auth/login`).error(new ProgressEvent('error'));
    expect(ok).toBe(false);
    expect(api.hasSession).toBe(false);
  });

  it('crea la reserva resolviendo servicio y terapeuta contra el catálogo', () => {
    localStorage.setItem('recovr-api-token', 'jwt-123');
    let id = '';
    api.reservar(REQUEST, 'RC-01', 'Ely').subscribe(r => (id = r.id));
    http.expectOne(`${API}/servicios`).flush([{ id: 3, nombre: 'RC-01 Masaje Tántrico Sensitivo', precio: 200 }]);
    http.expectOne(`${API}/empleados/catalogo`).flush([{ id: 2, nombre: 'Ely' }]);
    const post = http.expectOne(`${API}/db/reservas`);
    expect(post.request.body).toEqual({ servicioId: 3, empleadoId: 2, fechaHora: '2026-11-18T17:30:00' });
    expect(post.request.headers.get('Authorization')).toBe('Bearer jwt-123');
    post.flush({ id: 7, servicioNombre: 'RC-01', empleadoNombre: 'Ely', fechaHora: '2026-11-18T17:30:00', estado: 'PENDIENTE' });
    expect(id).toBe('REC-000007');
  });

  it('convierte un 409 de agenda en un error de negocio con el mensaje del servidor', () => {
    localStorage.setItem('recovr-api-token', 'jwt-123');
    let error: unknown;
    api.reservar(REQUEST, 'RC-01', 'Ely').subscribe({ error: e => (error = e) });
    http.expectOne(`${API}/servicios`).flush([{ id: 3, nombre: 'RC-01 Masaje', precio: 200 }]);
    http.expectOne(`${API}/empleados/catalogo`).flush([{ id: 2, nombre: 'Ely' }]);
    http.expectOne(`${API}/db/reservas`).flush({ message: 'El especialista seleccionado no se encuentra disponible' }, { status: 409, statusText: 'Conflict' });
    expect(error).toBeInstanceOf(ApiBusinessError);
    expect((error as Error).message).toContain('no se encuentra disponible');
  });

  it('expone el id numérico y el precio exacto del servidor para poder pagar la reserva completa', () => {
    localStorage.setItem('recovr-api-token', 'jwt-123');
    let reservaId: number | undefined;
    let price: number | undefined;
    api.reservar(REQUEST, 'RC-01', 'Ely').subscribe(r => { reservaId = r.reservaId; price = r.price; });
    http.expectOne(`${API}/servicios`).flush([{ id: 3, nombre: 'RC-01 Masaje', precio: 200 }]);
    http.expectOne(`${API}/empleados/catalogo`).flush([{ id: 2, nombre: 'Ely' }]);
    http.expectOne(`${API}/db/reservas`).flush({ id: 7, servicioNombre: 'RC-01', empleadoNombre: 'Ely',
      fechaHora: '2026-11-18T17:30:00', estado: 'PENDIENTE', precio: 200 });
    expect(reservaId).toBe(7);
    expect(price).toBe(200);
  });

  it('consulta la disponibilidad real de turnos de la terapeuta en una fecha', () => {
    localStorage.setItem('recovr-api-token', 'jwt-123');
    let turnos: { hora: string; disponible: boolean }[] = [];
    api.disponibilidad('RC-01', 'Ely', '2026-11-18').subscribe(t => (turnos = t));
    http.expectOne(`${API}/servicios`).flush([{ id: 3, nombre: 'RC-01 Masaje', precio: 200 }]);
    http.expectOne(`${API}/empleados/catalogo`).flush([{ id: 2, nombre: 'Ely' }]);
    const get = http.expectOne(`${API}/db/reservas/disponibilidad?empleadoId=2&servicioId=3&fecha=2026-11-18`);
    expect(get.request.headers.get('Authorization')).toBe('Bearer jwt-123');
    get.flush([{ hora: '18:00', disponible: false, motivo: 'Ely ya tiene una sesión a esa hora' }, { hora: '19:30', disponible: true, motivo: null }]);
    expect(turnos.map(t => t.disponible)).toEqual([false, true]);
  });

  it('paga la reserva por el importe completo y devuelve CONFIRMADA', () => {
    localStorage.setItem('recovr-api-token', 'jwt-123');
    let estado = '';
    api.pagarReserva(7, 200, 'YAPE').subscribe(r => (estado = r.status));
    const post = http.expectOne(`${API}/db/reservas/7/confirmar-y-pagar`);
    expect(post.request.body).toEqual({ monto: 200, metodoPago: 'YAPE' });
    post.flush({ id: 7, servicioNombre: 'RC-01', empleadoNombre: 'Ely', fechaHora: '2026-11-18T17:30:00', estado: 'CONFIRMADA', precio: 200 });
    expect(estado).toBe('CONFIRMADA');
  });

  it('muestra el mensaje del servidor si el pago se rechaza por no ser el total', () => {
    localStorage.setItem('recovr-api-token', 'jwt-123');
    let error: unknown;
    api.pagarReserva(7, 1, 'YAPE').subscribe({ error: e => (error = e) });
    http.expectOne(`${API}/db/reservas/7/confirmar-y-pagar`)
      .flush({ message: 'El pago debe ser el total de la sesión (S/ 200)' }, { status: 400, statusText: 'Bad Request' });
    expect(error).toBeInstanceOf(ApiBusinessError);
    expect((error as Error).message).toContain('total de la sesión');
  });
});
