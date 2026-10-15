import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { ApiService, PerfilApi } from './api.service';

/** Los cuatro roles reales del backend. No existen roles "locales": todo permiso lo decide el servidor. */
export type Rol = 'ADMIN' | 'RECEPCIONISTA' | 'ESPECIALISTA' | 'CLIENTE';

export type Vista = 'RESUMEN' | 'AGENDA' | 'CLIENTES' | 'CAJA' | 'PERSONAL' | 'CATALOGO' | 'USUARIOS' | 'FICHAS' | 'MI_PERFIL' | 'SISTEMA' | 'SEGURIDAD' | 'MI_CUENTA';

export interface SessionUser {
  correo: string;
  rol: Rol;
  nombre: string;
  clienteId: number | null;
  empleadoId: number | null;
  telefono: string | null;
  debeCambiarClave: boolean;
}

/** Qué vistas ve cada rol. Es solo comodidad de interfaz: el backend vuelve a validar cada petición. */
export const VISTAS_POR_ROL: Record<Rol, Vista[]> = {
  ADMIN: ['RESUMEN', 'AGENDA', 'CLIENTES', 'CAJA', 'PERSONAL', 'USUARIOS', 'CATALOGO', 'SISTEMA', 'SEGURIDAD'],
  RECEPCIONISTA: ['RESUMEN', 'AGENDA', 'CLIENTES', 'CAJA'],
  ESPECIALISTA: ['AGENDA', 'FICHAS', 'MI_PERFIL'],
  CLIENTE: ['MI_CUENTA']
};

const ROLES: Rol[] = ['ADMIN', 'RECEPCIONISTA', 'ESPECIALISTA', 'CLIENTE'];

/**
 * Sesión del usuario. La única fuente de verdad es el backend: se inicia sesión con /auth/login y la identidad y el
 * rol se confirman con /auth/me. No hay usuarios, contraseñas ni puntos guardados en el navegador; solo el token.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService);

  readonly user = signal<SessionUser | null>(null);
  private validacion: Observable<boolean> | null = null;

  constructor() {
    // Con un token guardado, se confirma con el servidor quién es antes de mostrar nada personalizado.
    if (this.api.hasSession) {
      this.asegurarSesion().subscribe();
    }
  }

  /** Inicia sesión y confirma el perfil con el servidor. */
  login(correo: string, password: string): Observable<{ ok: boolean; message?: string }> {
    return this.api.loginConMensaje(correo, password).pipe(switchMap(res => this.tras(res)));
  }

  registrar(nombre: string, correo: string, password: string, telefono: string): Observable<{ ok: boolean; message?: string }> {
    return this.api.registrarConMensaje(nombre, correo, password, telefono).pipe(switchMap(res => this.tras(res)));
  }

  private tras(res: { ok: boolean; message?: string }): Observable<{ ok: boolean; message?: string }> {
    if (!res.ok) {
      return of(res);
    }
    return this.confirmar().pipe(map(ok => ok ? { ok: true } : { ok: false, message: 'No se pudo validar la sesión.' }));
  }

  /** Pregunta al servidor quién es el dueño del token. Si no es válido, limpia la sesión. */
  private confirmar(): Observable<boolean> {
    return this.api.perfil().pipe(
      tap(p => this.user.set(this.aUsuario(p))),
      map(() => true),
      catchError(() => { this.cerrarLocal(); return of(false); })
    );
  }

  /** Para los guards: valida el token una sola vez por carga de página. */
  asegurarSesion(): Observable<boolean> {
    if (!this.api.hasSession) {
      this.user.set(null);
      return of(false);
    }
    if (this.user()) {
      return of(true);
    }
    this.validacion ??= this.confirmar().pipe(tap(() => (this.validacion = null)));
    return this.validacion;
  }

  /** Tras cambiar la contraseña se vuelve a pedir el perfil para apagar el aviso de "clave temporal". */
  refrescar(): Observable<boolean> {
    return this.confirmar();
  }

  logout(): void {
    this.cerrarLocal();
  }

  private cerrarLocal(): void {
    this.api.clearSession();
    this.user.set(null);
    this.validacion = null;
  }

  isAuthenticated(): boolean {
    return this.user() !== null;
  }

  isClient(): boolean {
    return this.user()?.rol === 'CLIENTE';
  }

  vistasPermitidas(): Vista[] {
    const rol = this.user()?.rol;
    return rol ? VISTAS_POR_ROL[rol] : [];
  }

  canAccess(vista: string): boolean {
    return (this.vistasPermitidas() as string[]).includes(vista);
  }

  vistaInicial(): Vista | null {
    return this.vistasPermitidas()[0] ?? null;
  }

  private aUsuario(p: PerfilApi): SessionUser {
    const rol = ROLES.includes(p.rol as Rol) ? (p.rol as Rol) : 'CLIENTE';
    return { correo: p.correo, rol, nombre: p.nombre, clienteId: p.clienteId, empleadoId: p.empleadoId, telefono: p.telefono, debeCambiarClave: p.debeCambiarClave };
  }
}

/** Solo entra quien tenga una sesión válida confirmada por el servidor. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.asegurarSesion().pipe(map(ok => ok ? true : router.createUrlTree(['/admin/login'])));
};
