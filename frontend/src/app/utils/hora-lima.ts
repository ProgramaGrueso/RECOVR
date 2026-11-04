/** Zona horaria en la que opera el negocio (la misma que usa el backend). */
export const ZONA_NEGOCIO = 'America/Lima';

const FORMATO = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONA_NEGOCIO, year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
});

/**
 * Fecha y hora de pared de Lima en ISO sin zona (yyyy-MM-ddTHH:mm:ss.SSS), el mismo formato que devuelve la API.
 * No depende de la zona horaria del navegador.
 */
export function isoLima(instante: Date = new Date()): string {
  const partes: Record<string, string> = {};
  for (const p of FORMATO.formatToParts(instante)) {
    partes[p.type] = p.value;
  }
  const ms = String(instante.getMilliseconds()).padStart(3, '0');
  return `${partes['year']}-${partes['month']}-${partes['day']}T${partes['hour']}:${partes['minute']}:${partes['second']}.${ms}`;
}

/**
 * "Ahora" como Date cuyos campos locales (getDate, getHours…) son la hora de Lima. Sirve para comparar con las
 * fechas sin zona que manda la API, que el navegador también interpreta como hora local.
 */
export function ahoraLima(instante: Date = new Date()): Date {
  return new Date(isoLima(instante));
}
