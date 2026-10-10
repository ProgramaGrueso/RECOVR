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
});
