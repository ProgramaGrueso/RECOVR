import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminApiService, OpsError } from './admin-api.service';
import { ApiService } from './api.service';

const API = 'http://localhost:8095/api';

describe('AdminApiService', () => {
  let admin: AdminApiService;
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
    admin = TestBed.inject(AdminApiService);
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function fallo(status: number, body: object | null = null): OpsError {
    let error!: OpsError;
    admin.cancelar(1).subscribe({ error: e => (error = e) });
    http.expectOne(`${API}/db/reservas/1/cancelar`).flush(body, { status, statusText: 'x' });
    return error;
  }

  it('envía el token en cada petición y usa PATCH para cancelar', () => {
    api.loginConMensaje('a@a.com', 'x').subscribe();
    http.expectOne(`${API}/auth/login`).flush({ token: 'T1', rol: 'ADMIN', correo: 'a@a.com' });
    expect(api.session()).toEqual({ role: 'ADMIN', correo: 'a@a.com' });

    admin.cancelar(7).subscribe();
    const req = http.expectOne(`${API}/db/reservas/7/cancelar`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.headers.get('Authorization')).toBe('Bearer T1');
    req.flush({});
  });

  it('un 409 muestra el mensaje de negocio del backend', () => {
    const e = fallo(409, { message: 'La suite o sala de tratamiento seleccionada ya se encuentra ocupada.' });
    expect(e.message).toContain('ya se encuentra ocupada');
    expect(e.status).toBe(409);
  });

  it('un 400 de validación lista los campos inválidos', () => {
    expect(fallo(400, { campos: { empleadoId: 'no debe ser nulo' } }).message).toContain('empleadoId: no debe ser nulo');
  });

  it('un 403 indica el rol real de la cuenta conectada', () => {
    api.loginConMensaje('r@r.com', 'x').subscribe();
    http.expectOne(`${API}/auth/login`).flush({ token: 'T2', rol: 'RECEPCIONISTA', correo: 'r@r.com' });
    expect(fallo(403).message).toContain('RECEPCIONISTA');
  });

  it('un 401 cierra la sesión para que el panel pida conectarse de nuevo', () => {
    api.loginConMensaje('a@a.com', 'x').subscribe();
    http.expectOne(`${API}/auth/login`).flush({ token: 'T3', rol: 'ADMIN', correo: 'a@a.com' });
    fallo(401);
    expect(api.session()).toBeNull();
  });

  it('sin conexión (status 0) pide encender el backend', () => {
    expect(fallo(0).message).toContain('Enciende el backend');
  });
});
