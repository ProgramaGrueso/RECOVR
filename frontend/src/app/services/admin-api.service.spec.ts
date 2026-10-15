import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminApiService, OpsError } from './admin-api.service';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';

@Component({ selector: 'app-vacio', template: '' })
class VacioComponent {}

const API = 'http://localhost:8095/api';

describe('AdminApiService', () => {
  let admin: AdminApiService;
  let api: ApiService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    const store = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (k: string) => store.get(k) ?? null,
      setItem: (k: string, v: string) => void store.set(k, v),
      removeItem: (k: string) => void store.delete(k),
      clear: () => store.clear()
    });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: 'admin/login', component: VacioComponent }])]
    });
    admin = TestBed.inject(AdminApiService);
    api = TestBed.inject(ApiService);
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Inicia sesión como lo hace la pantalla de acceso: /auth/login y luego /auth/me. */
  function iniciar(rol: string, token: string): void {
    auth.login('u@u.com', 'x').subscribe();
    http.expectOne(`${API}/auth/login`).flush({ token, rol, correo: 'u@u.com' });
    http.expectOne(`${API}/auth/me`).flush({ correo: 'u@u.com', rol, nombre: 'Usuaria', clienteId: null, empleadoId: null, telefono: null, debeCambiarClave: false });
  }

  function fallo(status: number, body: object | null = null): OpsError {
    let error!: OpsError;
    admin.cancelar(1).subscribe({ error: e => (error = e) });
    http.expectOne(`${API}/db/reservas/1/cancelar`).flush(body, { status, statusText: 'x' });
    return error;
  }

  it('envía el token en cada petición y usa PATCH para cancelar', () => {
    iniciar('ADMIN', 'T1');
    expect(auth.user()?.rol).toBe('ADMIN');

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
    iniciar('RECEPCIONISTA', 'T2');
    expect(fallo(403).message).toContain('RECEPCIONISTA');
  });

  it('un 401 cierra la sesión para que el panel pida conectarse de nuevo', () => {
    iniciar('ADMIN', 'T3');
    fallo(401);
    expect(api.session()).toBeNull();
    expect(auth.user()).toBeNull();
  });

  it('sin conexión (status 0) pide encender el backend', () => {
    expect(fallo(0).message).toContain('Enciende el backend');
  });
});
