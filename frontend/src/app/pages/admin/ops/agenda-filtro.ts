import { ReservaOps } from '../../../models/ops.model';

export type Rango = 'HOY' | 'MANANA' | 'SEMANA' | 'PASADAS' | 'TODAS';

export interface FiltroAgenda {
  rango: Rango;
  estado: string;
  salaId: string;
  texto: string;
}

const DIA_MS = 86_400_000;

/**
 * Reservas que se muestran en la agenda según el rango, el estado, la cabina y el texto buscado (cliente o servicio).
 * Los rangos futuros van en orden cronológico; "pasadas" y "todas", de la más reciente a la más antigua.
 * {@code ahora} es la hora de Lima como Date local (ver utils/hora-lima), igual que las fechas sin zona de la API.
 */
export function filtrarAgenda(reservas: ReservaOps[], filtro: FiltroAgenda, ahora: Date): ReservaOps[] {
  const inicioHoy = new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate()).getTime();
  const q = filtro.texto.trim().toLowerCase();
  const enRango = (r: ReservaOps): boolean => {
    const d = new Date(r.fechaHora).getTime() - inicioHoy;
    switch (filtro.rango) {
      case 'HOY': return d >= 0 && d < DIA_MS;
      case 'MANANA': return d >= DIA_MS && d < 2 * DIA_MS;
      case 'SEMANA': return d >= 0 && d < 7 * DIA_MS;
      case 'PASADAS': return d < 0;
      default: return true;
    }
  };
  const lista = reservas.filter(r => enRango(r)
    && (!filtro.estado || r.estadoOperativo === filtro.estado)
    && (!filtro.salaId || String(r.salaId) === filtro.salaId)
    && (!q || r.clienteNombre.toLowerCase().includes(q) || r.servicioNombre.toLowerCase().includes(q)));
  const asc = filtro.rango !== 'PASADAS' && filtro.rango !== 'TODAS';
  return lista.sort((a, b) => (asc ? 1 : -1) * a.fechaHora.localeCompare(b.fechaHora));
}
