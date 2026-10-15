import { EstadoOperativo } from '../../../models/ops.model';

export const ESTADO_TXT: Record<EstadoOperativo, string> = {
  EN_ESPERA: 'En espera',
  CONFIRMADA: 'Confirmada',
  EN_SESION: 'En sesión',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada'
};

export const SALA_ESTADO_TXT: Record<string, string> = {
  DISPONIBLE: 'Disponible',
  OCUPADA: 'Ocupada',
  ESPERANDO_CLIENTE: 'Esperando cliente',
  EN_SANITIZACION: 'En sanitización'
};

export const EMPLEADO_ESTADO_TXT: Record<string, string> = {
  LIBRE: 'Libre',
  EN_SESION: 'En sesión',
  POR_INICIAR: 'Por iniciar'
};

export const METODOS_PAGO = ['Yape', 'Plin', 'Efectivo', 'Tarjeta'];

export function horaTxt(iso: string | null): string {
  return iso ? new Date(iso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false }) : '';
}

export function fechaTxt(iso: string): string {
  return new Date(iso).toLocaleDateString('es-PE', { weekday: 'short', day: '2-digit', month: 'short' });
}

export function fechaHoraTxt(iso: string | null): string {
  return iso ? `${fechaTxt(iso)} · ${horaTxt(iso)}` : '—';
}

/** Fecha local en formato yyyy-mm-dd (la que usa <input type="date">). */
export function fechaInput(d: Date = new Date()): string {
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

export function soles(n: number | null | undefined): string {
  return 'S/ ' + Number(n ?? 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function codigoReserva(id: number): string {
  return 'REC-' + String(id).padStart(6, '0');
}
