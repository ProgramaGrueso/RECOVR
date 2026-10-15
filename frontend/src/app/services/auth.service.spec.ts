import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { AuthService, VISTAS_POR_ROL } from './auth.service';
import { ApiService } from './api.service';

const API = 'http://localhost:8095/api';

describe('AuthService', () => {
  let auth: AuthService;
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
    auth = TestBed.inject(AuthService);
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function perfil(rol: string, nombre = 'Ana'): object {
    return { correo: 'a@a.com', rol, nombre, clienteId: null, empleadoId: null, telefono: null, debeCambiarClave: false };
  }

  it('credenciales incorrectas: no hay sesión y se explica el motivo', async () => {
    const p = firstValueFrom(auth.login('a@a.com', 'mala'));
    http.expectOne(`${API}/auth/login`).flush({ message: 'x' }, { status: 401, statusText: 'No' });
    const r = await p;
    expect(r.ok).toBe(false);
    expect(r.message).toContain('incorrectos');
    expect(auth.user()).toBeNull();
    expect(api.hasSession).toBe(false);
  });

  it('un bloqueo por intentos (429) muestra el mensaje del servidor', async () => {
    const p = firstValueFrom(auth.login('a@a.com', 'x'));
    http.expectOne(`${API}/auth/login`).flush({ message: 'Demasiados intentos fallidos. Espera unos minutos.' }, { status: 429, statusText: 'Too Many' });
    expect((await p).message).toContain('Demasiados intentos');
  });

  it('el rol sale de /auth/me (servidor), no de lo que diga el navegador', async () => {
    const p = firstValueFrom(auth.login('a@a.com', 'Clave1234'));
    http.expectOne(`${API}/auth/login`).flush({ token: 'T', rol: 'ADMIN', correo: 'a@a.com' });
    http.expectOne(`${API}/auth/me`).flush(perfil('CLIENTE'));
    expect((await p).ok).toBe(true);
    expect(auth.user()?.rol).toBe('CLIENTE');
    expect(auth.vistasPermitidas()).toEqual(VISTAS_POR_ROL.CLIENTE);
  });

  it('cada rol ve solo sus módulos', async () => {
    const p = firstValueFrom(auth.login('a@a.com', 'Clave1234'));
    http.expectOne(`${API}/auth/login`).flush({ token: 'T', rol: 'RECEPCIONISTA', correo: 'a@a.com' });
    http.expectOne(`${API}/auth/me`).flush(perfil('RECEPCIONISTA'));
    await p;
    expect(auth.canAccess('AGENDA')).toBe(true);
    expect(auth.canAccess('PERSONAL')).toBe(false);
    expect(auth.canAccess('SISTEMA')).toBe(false);
    expect(auth.canAccess('FICHAS')).toBe(false);
    expect(VISTAS_POR_ROL.ESPECIALISTA).toEqual(['AGENDA', 'FICHAS', 'MI_PERFIL']);
    expect(VISTAS_POR_ROL.ADMIN).toContain('USUARIOS');
    expect(VISTAS_POR_ROL.CLIENTE).toEqual(['MI_CUENTA']);
    expect(VISTAS_POR_ROL.ADMIN).not.toContain('FICHAS');
  });

  it('un token inválido (401 en /auth/me) cierra la sesión', async () => {
    const p = firstValueFrom(auth.login('a@a.com', 'Clave1234'));
    http.expectOne(`${API}/auth/login`).flush({ token: 'T', rol: 'ADMIN', correo: 'a@a.com' });
    http.expectOne(`${API}/auth/me`).flush({}, { status: 401, statusText: 'No' });
    expect((await p).ok).toBe(false);
    expect(auth.user()).toBeNull();
    expect(api.hasSession).toBe(false);
  });

  it('logout borra la sesión', async () => {
    const p = firstValueFrom(auth.login('a@a.com', 'Clave1234'));
    http.expectOne(`${API}/auth/login`).flush({ token: 'T', rol: 'ADMIN', correo: 'a@a.com' });
    http.expectOne(`${API}/auth/me`).flush(perfil('ADMIN'));
    await p;
    auth.logout();
    expect(auth.user()).toBeNull();
    expect(api.hasSession).toBe(false);
  });
});
