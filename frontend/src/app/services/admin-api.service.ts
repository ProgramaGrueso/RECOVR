import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { ApiService } from './api.service';
import {
  AuditoriaOps, CanjeOps, CanjeValidoOps, ClienteOps, EmpleadoEstado, EmpleadoOps, EstadoSistemaOps, FichaForm, FichaOps, MiClienteOps,
  AusenciaOps, MiCuentaOps, MiPerfilOps, PermisoOps, PuntosOps, ReservaOps, ResumenOps, SalaEstado, SalaOps, ServicioOps, UsuarioNuevoOps, UsuarioOps
} from '../models/ops.model';

/** Error con un mensaje listo para mostrar al personal. */
export class OpsError extends Error {
  constructor(message: string, readonly status = 0) {
    super(message);
  }
}

/**
 * Todas las operaciones de la zona administrativa contra la API real. Cada método devuelve un Observable y,
 * si falla, un OpsError con el motivo en español (409 agenda ocupada, 400 regla de negocio, 403 sin permiso...).
 */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private http = inject(HttpClient);
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);

  private req<T>(method: string, path: string, body?: unknown): Observable<T> {
    return this.http.request<T>(method, `${this.api.baseUrl}${path}`, { body, headers: this.api.authHeaders() }).pipe(
      catchError((err: unknown) => throwError(() => this.traducir(err)))
    );
  }

  private traducir(err: unknown): OpsError {
    if (!(err instanceof HttpErrorResponse)) {
      return new OpsError('Ocurrió un error inesperado.');
    }
    if (err.status === 0) {
      return new OpsError('No hay conexión con el servidor. Enciende el backend (puerto 8095).', 0);
    }
    if (err.status === 401) {
      this.auth.logout();
      void this.router.navigate(['/admin/login']);
      return new OpsError('Tu sesión expiró. Vuelve a iniciar sesión.', 401);
    }
    if (err.status === 403) {
      const rol = this.auth.user()?.rol || 'desconocido';
      return new OpsError(`Tu rol (${rol}) no tiene permiso para esta acción.`, 403);
    }
    const campos = err.error?.campos ? Object.entries(err.error.campos).map(([k, v]) => `${k}: ${v}`).join(' · ') : '';
    return new OpsError(campos || err.error?.message || `Error ${err.status} del servidor.`, err.status);
  }

  // ---- Reservas ----
  reservas(): Observable<ReservaOps[]> { return this.req('GET', '/db/reservas'); }
  miAgenda(): Observable<ReservaOps[]> { return this.req('GET', '/db/reservas/agenda'); }
  crearReserva(body: { clienteId: number; empleadoId: number; servicioId: number; salaId: number | null; fechaHora: string }): Observable<ReservaOps> {
    return this.req('POST', '/db/reservas', body);
  }
  editarReserva(id: number, body: { clienteId: number; empleadoId: number; servicioId: number; salaId: number | null; fechaHora: string }): Observable<ReservaOps> {
    return this.req('PUT', `/db/reservas/${id}`, body);
  }
  cobrarYConfirmar(id: number, monto: number, metodoPago: string, codigoCanje?: string): Observable<ReservaOps> {
    return this.req('POST', `/db/reservas/${id}/confirmar-y-pagar`, { monto, metodoPago, ...(codigoCanje ? { codigoCanje } : {}) });
  }
  validarCanje(codigo: string, reservaId: number): Observable<CanjeValidoOps> {
    return this.req('GET', `/canjes/${encodeURIComponent(codigo.trim())}?reservaId=${reservaId}`);
  }
  checkIn(id: number): Observable<ReservaOps> { return this.req('POST', `/db/reservas/${id}/check-in`); }
  finalizar(id: number): Observable<ReservaOps> { return this.req('POST', `/db/reservas/${id}/finalizar`); }
  reabrir(id: number): Observable<ReservaOps> { return this.req('POST', `/db/reservas/${id}/reabrir`); }
  cancelar(id: number): Observable<ReservaOps> { return this.req('PATCH', `/db/reservas/${id}/cancelar`); }
  eliminarReserva(id: number): Observable<void> { return this.req('DELETE', `/db/reservas/${id}`); }

  // ---- Clientes ----
  clientes(): Observable<ClienteOps[]> { return this.req('GET', '/clientes'); }
  crearCliente(b: { nombre: string; correo: string; telefono: string }): Observable<ClienteOps> { return this.req('POST', '/clientes', b); }
  editarCliente(id: number, b: { nombre: string; correo: string; telefono: string }): Observable<ClienteOps> { return this.req('PUT', `/clientes/${id}`, b); }
  eliminarCliente(id: number): Observable<void> { return this.req('DELETE', `/clientes/${id}`); }

  // ---- Personal (masajistas) ----
  crearEmpleado(b: { nombre: string; especialidad: string; telefono: string; bio: string }): Observable<EmpleadoOps> { return this.req('POST', '/empleados', b); }
  editarEmpleado(id: number, b: { nombre: string; especialidad: string; telefono: string; bio: string }): Observable<EmpleadoOps> { return this.req('PUT', `/empleados/${id}`, b); }
  cambiarActivoEmpleado(id: number, valor: boolean): Observable<EmpleadoOps> { return this.req('PATCH', `/empleados/${id}/activo?valor=${valor}`); }
  eliminarEmpleado(id: number): Observable<void> { return this.req('DELETE', `/empleados/${id}`); }
  subirFotoEmpleado(id: number, foto: Blob): Observable<EmpleadoOps> { return this.req('PUT', `/empleados/${id}/foto`, this.formulario(foto)); }
  quitarFotoEmpleado(id: number): Observable<void> { return this.req('DELETE', `/empleados/${id}/foto`); }

  // ---- Accesos del personal (ADMIN) ----
  usuarios(rol?: string): Observable<UsuarioOps[]> { return this.req('GET', `/usuarios${rol ? `?rol=${rol}` : ''}`); }
  crearUsuario(b: { nombre: string; correo: string; rol: string; empleadoId: number | null }): Observable<UsuarioNuevoOps> { return this.req('POST', '/usuarios', b); }
  activarUsuario(id: number, valor: boolean): Observable<UsuarioOps> { return this.req('PATCH', `/usuarios/${id}/activo?valor=${valor}`); }
  restablecerClave(id: number): Observable<UsuarioNuevoOps> { return this.req('POST', `/usuarios/${id}/restablecer-clave`); }
  cambiarClave(claveActual: string, claveNueva: string): Observable<void> { return this.req('POST', '/auth/cambiar-clave', { claveActual, claveNueva }); }

  // ---- Perfil de la terapeuta ----
  miPerfil(): Observable<MiPerfilOps> { return this.req('GET', '/mi-perfil'); }
  actualizarMiPerfil(b: { telefono: string; bio: string }): Observable<MiPerfilOps> { return this.req('PUT', '/mi-perfil', b); }
  subirMiFoto(foto: Blob): Observable<MiPerfilOps> { return this.req('PUT', '/mi-perfil/foto', this.formulario(foto)); }
  quitarMiFoto(): Observable<MiPerfilOps> { return this.req('DELETE', '/mi-perfil/foto'); }

  /** URL de la foto pública de una terapeuta (la API devuelve una ruta relativa a su origen). */
  urlFoto(id: number, version = ''): string {
    return `${new URL(this.api.baseUrl).origin}/api/publico/terapeutas/${id}/foto${version ? `?v=${version}` : ''}`;
  }

  private formulario(foto: Blob): FormData {
    const f = new FormData();
    f.append('archivo', foto, 'foto.jpg');
    return f;
  }

  // ---- Servicios y cabinas ----
  servicios(): Observable<ServicioOps[]> { return this.req('GET', '/servicios'); }
  crearServicio(b: Omit<ServicioOps, 'id'>): Observable<ServicioOps> { return this.req('POST', '/servicios', b); }
  editarServicio(id: number, b: Omit<ServicioOps, 'id'>): Observable<ServicioOps> { return this.req('PUT', `/servicios/${id}`, b); }
  eliminarServicio(id: number): Observable<void> { return this.req('DELETE', `/servicios/${id}`); }
  crearSala(b: { nombre: string; capacidad: number }): Observable<SalaOps> { return this.req('POST', '/salas', b); }
  editarSala(id: number, b: { nombre: string; capacidad: number }): Observable<SalaOps> { return this.req('PUT', `/salas/${id}`, b); }
  eliminarSala(id: number): Observable<void> { return this.req('DELETE', `/salas/${id}`); }
  cambiarActivaSala(id: number, valor: boolean): Observable<SalaOps> { return this.req('PATCH', `/salas/${id}/activa?valor=${valor}`); }
  ausencias(empleadoId: number): Observable<AusenciaOps[]> { return this.req('GET', `/empleados/${empleadoId}/ausencias`); }
  crearAusencia(empleadoId: number, b: { desde: string; hasta: string; motivo: string }): Observable<AusenciaOps> {
    return this.req('POST', `/empleados/${empleadoId}/ausencias`, b);
  }
  eliminarAusencia(ausenciaId: number): Observable<void> { return this.req('DELETE', `/empleados/ausencias/${ausenciaId}`); }

  // ---- Puntos y canjes ----
  puntos(): Observable<PuntosOps> { return this.req('GET', '/mi-cuenta/puntos'); }
  anularCanje(codigo: string): Observable<CanjeOps> { return this.req('POST', `/mi-cuenta/canjes/${encodeURIComponent(codigo)}/anular`); }
  canjear(beneficioId: string): Observable<CanjeOps> { return this.req('POST', '/mi-cuenta/canjes', { beneficioId }); }

  // ---- Portal del cliente ----
  miCuenta(): Observable<MiCuentaOps> { return this.req('GET', '/mi-cuenta/resumen'); }
  actualizarMiCuenta(b: { nombre: string; telefono: string }): Observable<MiCuentaOps> { return this.req('PUT', '/mi-cuenta', b); }
  misReservas(): Observable<ReservaOps[]> { return this.req('GET', '/db/reservas/mias'); }

  // ---- Fichas de clientes (terapeutas) ----
  misClientes(): Observable<MiClienteOps[]> { return this.req('GET', '/fichas/mis-clientes'); }
  ficha(clienteId: number): Observable<FichaOps> { return this.req('GET', `/fichas/cliente/${clienteId}`); }
  guardarFicha(clienteId: number, b: FichaForm): Observable<FichaOps> { return this.req('PUT', `/fichas/cliente/${clienteId}`, b); }
  agregarNota(clienteId: number, b: { tratamiento: string; observaciones: string; recomendaciones: string }): Observable<FichaOps> {
    return this.req('POST', `/fichas/cliente/${clienteId}/notas`, b);
  }
  eliminarNota(notaId: number): Observable<void> { return this.req('DELETE', `/fichas/notas/${notaId}`); }

  // ---- Sistema y seguridad (ADMIN) ----
  estadoSistema(): Observable<EstadoSistemaOps> { return this.req('GET', '/sistema/estado'); }
  auditoria(limite = 100): Observable<AuditoriaOps[]> { return this.req('GET', `/sistema/auditoria?limite=${limite}`); }
  matriz(): Observable<PermisoOps[]> { return this.req('GET', '/seguridad/matriz'); }

  // ---- Estado en vivo e indicadores ----
  estadoSalas(): Observable<SalaEstado[]> { return this.req('GET', '/operacion/salas'); }
  estadoEmpleados(): Observable<EmpleadoEstado[]> { return this.req('GET', '/operacion/empleados'); }
  resumen(): Observable<ResumenOps> { return this.req('GET', '/operacion/resumen'); }
}
