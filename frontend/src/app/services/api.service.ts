import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap, throwError } from 'rxjs';
import { BookingRequest, BookingResponse, TurnoDisponible } from '../models/booking.model';

const TOKEN_KEY = 'recovr-api-token';
const URL_KEY = 'recovr-api-url';
const ROLE_KEY = 'recovr-api-role';
const EMAIL_KEY = 'recovr-api-email';
const DEFAULT_API_URL = 'http://localhost:8095/api';

interface ServicioApi { id: number; nombre: string; precio: number; }
interface EmpleadoApi { id: number; nombre: string; }
interface ReservaApi {
  id: number;
  servicioNombre: string;
  empleadoNombre: string;
  fechaHora: string;
  estado: BookingResponse['status'];
  precio?: number;
}

export interface PerfilApi {
  correo: string; rol: string; nombre: string;
  clienteId: number | null; empleadoId: number | null; telefono: string | null;
  debeCambiarClave: boolean;
}

/** Error de negocio devuelto por la API (409 agenda ocupada, 400 validación...). */
export class ApiBusinessError extends Error {}

/**
 * Cliente de la API REST Spring Boot. Guarda el JWT del backend y crea las reservas reales.
 * La URL se puede cambiar con localStorage['recovr-api-url'] (por defecto http://localhost:8095/api).
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  /** Sesión con el backend (rol y correo del token). Es una señal para que el panel reaccione al conectar/cerrar. */
  private readonly sessionState = signal<{ role: string; correo: string } | null>(this.loadSession());
  readonly session = this.sessionState.asReadonly();

  private loadSession(): { role: string; correo: string } | null {
    try {
      const token = localStorage.getItem(TOKEN_KEY);
      return token ? { role: localStorage.getItem(ROLE_KEY) ?? '', correo: localStorage.getItem(EMAIL_KEY) ?? '' } : null;
    } catch {
      return null;
    }
  }

  get baseUrl(): string {
    return this.base;
  }

  authHeaders(): HttpHeaders {
    return this.headers();
  }

  private get base(): string {
    try {
      return localStorage.getItem(URL_KEY) || DEFAULT_API_URL;
    } catch {
      return DEFAULT_API_URL;
    }
  }

  get hasSession(): boolean {
    return !!this.token;
  }

  private get token(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  clearSession(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(ROLE_KEY);
      localStorage.removeItem(EMAIL_KEY);
    } catch { /* sin almacenamiento */ }
    this.sessionState.set(null);
  }

  private saveToken(token: string, role = '', correo = ''): void {
    try {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(ROLE_KEY, role);
      localStorage.setItem(EMAIL_KEY, correo);
    } catch { /* sin almacenamiento */ }
    this.sessionState.set({ role, correo });
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.token ?? ''}` });
  }

  /** Inicia sesión en el backend; devuelve false si no hay conexión o las credenciales no existen. */
  login(correo: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string; rol?: string; correo?: string }>(`${this.base}/auth/login`, { correo, password }).pipe(
      map(res => { this.saveToken(res.token, res.rol ?? '', res.correo ?? correo); return true; }),
      catchError(() => of(false))
    );
  }

  /** Igual que login, pero explica el motivo del fallo (pantalla de acceso). */
  loginConMensaje(correo: string, password: string): Observable<{ ok: boolean; message?: string }> {
    return this.http.post<{ token: string; rol?: string; correo?: string }>(`${this.base}/auth/login`, { correo, password }).pipe(
      map(res => { this.saveToken(res.token, res.rol ?? '', res.correo ?? correo); return { ok: true }; }),
      catchError((err: HttpErrorResponse) => of({ ok: false, message: this.mensajeDeAcceso(err) }))
    );
  }

  /** Crea la cuenta de cliente y deja la sesión iniciada. Devuelve el motivo si el servidor la rechaza. */
  registrarConMensaje(nombre: string, correo: string, password: string, telefono: string): Observable<{ ok: boolean; message?: string }> {
    return this.http.post<{ token: string; rol?: string; correo?: string }>(`${this.base}/auth/registro`, { nombre, correo, password, telefono }).pipe(
      map(res => { this.saveToken(res.token, res.rol ?? 'CLIENTE', res.correo ?? correo); return { ok: true }; }),
      catchError((err: HttpErrorResponse) => of({ ok: false, message: this.mensajeDeAcceso(err) }))
    );
  }

  /** Perfil del token guardado: confirma con el servidor quién es y qué rol tiene (no se confía en lo guardado en el navegador). */
  perfil(): Observable<PerfilApi> {
    return this.http.get<PerfilApi>(`${this.base}/auth/me`, { headers: this.headers() });
  }

  private mensajeDeAcceso(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No hay conexión con el servidor. Enciende el backend (puerto 8095) e inténtalo de nuevo.';
    }
    if (err.status === 429) {
      return err.error?.message ?? 'Demasiados intentos fallidos. Espera unos minutos.';
    }
    if (err.status === 401) {
      return 'Correo o contraseña incorrectos.';
    }
    const campos = err.error?.campos ? Object.values(err.error.campos).join(' · ') : '';
    return campos || err.error?.message || 'No se pudo completar la operación.';
  }

  /**
   * Crea la reserva real. Los ids del front (rc-01, Ely...) se resuelven contra el catálogo del backend
   * y la cabina la asigna el servidor. Un conflicto de agenda llega como ApiBusinessError.
   */
  reservar(request: BookingRequest, servicioCodigo: string, terapeuta: string): Observable<BookingResponse> {
    const opts = { headers: this.headers() };
    return this.http.get<ServicioApi[]>(`${this.base}/servicios`, opts).pipe(
      switchMap(servicios => this.http.get<EmpleadoApi[]>(`${this.base}/empleados/catalogo`, opts).pipe(
        map(empleados => ({ servicios, empleados }))
      )),
      switchMap(({ servicios, empleados }) => {
        const servicio = servicios.find(s => s.nombre.toUpperCase().startsWith(servicioCodigo.toUpperCase()));
        const empleado = empleados.find(e => e.nombre.toLowerCase() === terapeuta.toLowerCase());
        if (!servicio || !empleado) {
          return throwError(() => new ApiBusinessError('El servicio o la terapeuta elegida no existe en el sistema.'));
        }
        return this.http.post<ReservaApi>(`${this.base}/db/reservas`, {
          servicioId: servicio.id,
          empleadoId: empleado.id,
          fechaHora: `${request.date}T${request.timeSlot}:00`
        }, opts);
      }),
      map(r => this.aBooking(r)),
      catchError((err: unknown) => throwError(() => this.traducir(err)))
    );
  }

  private aBooking(r: ReservaApi): BookingResponse {
    return {
      id: 'REC-' + String(r.id).padStart(6, '0'),
      status: r.estado,
      createdAt: new Date().toISOString(),
      serviceName: r.servicioNombre,
      professionalName: r.empleadoNombre,
      date: r.fechaHora.split('T')[0],
      timeSlot: r.fechaHora.split('T')[1].substring(0, 5),
      reservaId: r.id,
      price: r.precio
    };
  }

  /** Turnos del día con su disponibilidad real para esa terapeuta y servicio (el servidor explica los que no se pueden). */
  disponibilidad(servicioCodigo: string, terapeuta: string, fecha: string): Observable<TurnoDisponible[]> {
    const opts = { headers: this.headers() };
    return this.http.get<ServicioApi[]>(`${this.base}/servicios`, opts).pipe(
      switchMap(servicios => this.http.get<EmpleadoApi[]>(`${this.base}/empleados/catalogo`, opts).pipe(
        map(empleados => ({ servicios, empleados }))
      )),
      switchMap(({ servicios, empleados }) => {
        const servicio = servicios.find(s => s.nombre.toUpperCase().startsWith(servicioCodigo.toUpperCase()));
        const empleado = empleados.find(e => e.nombre.toLowerCase() === terapeuta.toLowerCase());
        if (!servicio || !empleado) {
          return throwError(() => new ApiBusinessError('El servicio o la terapeuta elegida no existe en el sistema.'));
        }
        return this.http.get<TurnoDisponible[]>(
          `${this.base}/db/reservas/disponibilidad?empleadoId=${empleado.id}&servicioId=${servicio.id}&fecha=${fecha}`, opts);
      }),
      catchError((err: unknown) => throwError(() => this.traducir(err)))
    );
  }

  /**
   * Paga la reserva por el importe completo (el servidor rechaza cualquier otro monto) y la deja CONFIRMADA.
   * No hay pasarela de pagos conectada: el método elegido queda registrado tal cual.
   */
  pagarReserva(reservaId: number, monto: number, metodoPago: string): Observable<BookingResponse> {
    return this.http.post<ReservaApi>(`${this.base}/db/reservas/${reservaId}/confirmar-y-pagar`,
      { monto, metodoPago }, { headers: this.headers() }).pipe(
      map(r => this.aBooking(r)),
      catchError((err: unknown) => throwError(() => this.traducir(err)))
    );
  }

  private traducir(err: unknown): unknown {
    if (err instanceof HttpErrorResponse && err.status >= 400 && err.status < 500 && err.status !== 401 && err.status !== 403) {
      return new ApiBusinessError(err.error?.message ?? 'La reserva no pudo registrarse.');
    }
    return err;
  }
}
