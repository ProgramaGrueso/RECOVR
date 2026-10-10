import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap, throwError } from 'rxjs';
import { BookingRequest, BookingResponse } from '../models/booking.model';

const TOKEN_KEY = 'recovr-api-token';
const URL_KEY = 'recovr-api-url';
const DEFAULT_API_URL = 'http://localhost:8080/api';

interface ServicioApi { id: number; nombre: string; precio: number; }
interface EmpleadoApi { id: number; nombre: string; }
interface ReservaApi {
  id: number;
  servicioNombre: string;
  empleadoNombre: string;
  fechaHora: string;
  estado: BookingResponse['status'];
}

/** Error de negocio devuelto por la API (409 agenda ocupada, 400 validación...). */
export class ApiBusinessError extends Error {}

/**
 * Cliente de la API REST Spring Boot. Guarda el JWT del backend y crea las reservas reales.
 * La URL se puede cambiar con localStorage['recovr-api-url'] (por defecto http://localhost:8080/api).
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

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
    try { localStorage.removeItem(TOKEN_KEY); } catch { /* sin almacenamiento */ }
  }

  private saveToken(token: string): void {
    try { localStorage.setItem(TOKEN_KEY, token); } catch { /* sin almacenamiento */ }
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.token ?? ''}` });
  }

  /** Inicia sesión en el backend; devuelve false si no hay conexión o las credenciales no existen. */
  login(correo: string, password: string): Observable<boolean> {
    return this.http.post<{ token: string }>(`${this.base}/auth/login`, { correo, password }).pipe(
      map(res => { this.saveToken(res.token); return true; }),
      catchError(() => of(false))
    );
  }

  /** Registra al cliente en el backend y deja la sesión iniciada (si ya existía, solo inicia sesión). */
  registrar(nombre: string, correo: string, password: string, telefono: string): Observable<boolean> {
    return this.http.post<{ token: string }>(`${this.base}/auth/registro`, { nombre, correo, password, telefono }).pipe(
      map(res => { this.saveToken(res.token); return true; }),
      catchError(() => this.login(correo, password))
    );
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
      map(r => ({
        id: 'REC-' + String(r.id).padStart(6, '0'),
        status: r.estado,
        createdAt: new Date().toISOString(),
        serviceName: r.servicioNombre,
        professionalName: r.empleadoNombre,
        date: r.fechaHora.split('T')[0],
        timeSlot: r.fechaHora.split('T')[1].substring(0, 5)
      })),
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
