import { ServiceItem } from '../../models/service.model';

/**
 * Órdenes cortas (escritas o habladas) que la página resuelve sola, sin gastar la IA:
 * ir a una sección, ver o reservar un tratamiento, ingresar, o hablar por WhatsApp.
 * Lo que no se reconoce (precios, horarios, dudas) lo responde la asistente con IA.
 */

export const WHATSAPP = '51924040992';

export type Orden =
  | { tipo: 'seccion'; id: string; texto: string }
  | { tipo: 'ver'; servicio: ServiceItem; texto: string }
  | { tipo: 'reservar'; servicio?: ServiceItem; texto: string }
  | { tipo: 'ruta'; ruta: string; texto: string }
  | { tipo: 'enlace'; url: string; etiqueta: string; texto: string };

const SECCIONES: Array<[string, RegExp, string]> = [
  ['ositos', /osit|\boso|gomita|gummy/, 'los ositos'],
  ['masajes', /tratamiento|masaje|carta|servicio|sesion/, 'los tratamientos'],
  ['cabinas', /cabina|sala|espacio|suite/, 'las cabinas'],
  ['staff', /masajista|terapeuta|staff|equipo/, 'las masajistas'],
  ['resenas', /resena|testimonio|opinion|comentario/, 'las reseñas'],
  ['ubicacion', /ubicaci|contacto|direccion|mapa|donde (queda|estan)|como llego|estacionamiento/, 'la ubicación'],
];

const VACIAS = new Set(['masaje', 'recovr', 'con', 'del', 'los', 'las', 'para']);

export const limpiar = (t: string): string => t.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

/** Tratamiento citado: gana el código o el nombre completo; si no, el que comparta más palabras. */
export function tratamientoCitado(t: string, servicios: ServiceItem[]): ServiceItem | null {
  let mejor: { s: ServiceItem; puntos: number } | null = null;
  for (const s of servicios) {
    const nombre = limpiar(s.name).replace(/&/g, ' ').replace(/\s+/g, ' ').trim();
    const sinasaje = nombre.replace(/^masaje\s+/, '');
    const palabras = sinasaje.split(' ').filter(p => p.length >= 3 && !VACIAS.has(p));
    let puntos = 0;
    if (t.includes(limpiar(s.code))) puntos = 300;
    else if (t.includes(nombre) || t.includes(sinasaje)) puntos = 100 + nombre.length;
    else puntos = palabras.filter(p => t.includes(p)).length * 10 + (/\b(4|cuatro) manos\b/.test(t) && /cuatro manos/.test(nombre) ? 20 : 0);
    if (puntos > 0 && (!mejor || puntos > mejor.puntos)) mejor = { s, puntos };
  }
  return mejor?.s ?? null;
}

export function interpretar(texto: string, servicios: ServiceItem[]): Orden | null {
  const t = limpiar(texto);
  if (t.length > 80) return null;

  if (/whatsapp|hablar con (una )?persona|asesor|recepcion(ista)?\b|humano/.test(t)) {
    const url = `https://wa.me/${WHATSAPP}?text=${encodeURIComponent('Hola, quisiera información de RECOVR.')}`;
    return { tipo: 'enlace', url, etiqueta: 'Abrir WhatsApp', texto: 'Claro, aquí puedes escribirle a una persona de RECOVR por WhatsApp.' };
  }

  if (/\b(iniciar sesion|inicia sesion|ingresar|login|mi cuenta|crear (una )?cuenta|registrarme)\b/.test(t)) {
    return { tipo: 'ruta', ruta: '/admin/login', texto: 'Listo, te llevo a ingresar con tu cuenta (o crear una).' };
  }

  const servicio = tratamientoCitado(t, servicios);
  const quiereReservar = /\b(reserva|reservar|reservame|agendar|agendame|separar|quiero el|quiero un|me quedo con)\b/.test(t);
  if (quiereReservar && servicio) {
    return { tipo: 'reservar', servicio, texto: `Perfecto, abrí la reserva de ${servicio.name}. Elige terapeuta y turno, y completa el pago para asegurarlo.` };
  }
  if (/^(quiero |deseo |me gustaria )?(reservar|agendar)( una)?( cita| sesion)?$/.test(t)) {
    return { tipo: 'reservar', texto: 'Perfecto, abrí la reserva. Elige tratamiento, terapeuta y turno.' };
  }

  if (!/\b(ve|ven|vamos|ir|ver|llevame|muestrame|ensename|bajame|abre|abreme|quiero ver|jugar)\b/.test(t)) return null;
  if (/horario|precio|fecha|disponib|cuanto|cuesta|turno|hay\b/.test(t)) return null; // eso lo responde la asistente

  if (servicio) return { tipo: 'ver', servicio, texto: `Claro, te muestro ${servicio.name}.` };
  const seccion = SECCIONES.find(([, patron]) => patron.test(t));
  return seccion ? { tipo: 'seccion', id: seccion[0], texto: `Claro, te llevo a ${seccion[2]}.` } : null;
}
