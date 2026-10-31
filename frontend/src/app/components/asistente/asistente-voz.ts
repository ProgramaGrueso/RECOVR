/**
 * Voz de la asistente (traída de Wimbledon): habla con la síntesis de voz del navegador (es-CO, instantánea, sin
 * servidor) y escucha con el reconocimiento de voz del navegador. Todo es opcional: si el navegador no lo soporta,
 * los botones no aparecen y el chat sigue en texto.
 */

export interface Voz {
  cortar(): void;
  empezar(): { id: number };
  decir(texto: string, turno: { id: number } | null): void;
  fallo(turno: { id: number } | null): void;
}

export const vozDisponible = (): boolean =>
  typeof window !== 'undefined' && 'speechSynthesis' in window && typeof SpeechSynthesisUtterance !== 'undefined';

/** Mejor voz en español: colombiana (Google o la del sistema) → otra de Google en español → cualquiera en español. */
function elegirVozEspanol(): SpeechSynthesisVoice | null {
  const voces = speechSynthesis.getVoices().filter(v => /^es\b|^es[-_]/i.test(v.lang));
  const google = (v: SpeechSynthesisVoice) => /google/i.test(v.name);
  const co = voces.filter(v => /^es[-_]CO$/i.test(v.lang));
  return co.find(google) || co[0] || voces.find(v => google(v) && /^es[-_](US|MX|419)$/i.test(v.lang))
    || voces.find(google) || voces[0] || null;
}

export function limpiarParaVoz(texto: string): string {
  return texto
    .replace(/[*_`#>]/g, '')
    .replace(/^\s*[-•]\s+/gm, '')
    .replace(/\p{Extended_Pictographic}/gu, '')
    .replace(/\s+/g, ' ')
    .trim();
}

export function crearVoz(alHablar: (hablando: boolean) => void): Voz {
  let turnoActual = 0;
  const cortar = () => { turnoActual++; speechSynthesis.cancel(); alHablar(false); };
  speechSynthesis.getVoices(); // Chrome las carga en segundo plano
  return {
    cortar,
    empezar() { cortar(); return { id: turnoActual }; },
    decir(texto, turno) {
      if (!turno || turno.id !== turnoActual) return;
      // Por frases: Chrome corta los textos largos a la mitad.
      const frases = limpiarParaVoz(texto).split(/(?<=[.!?…:])\s+/).map(f => f.trim()).filter(Boolean);
      if (!frases.length) return;
      const voz = elegirVozEspanol();
      frases.forEach((f, i) => {
        const u = new SpeechSynthesisUtterance(f);
        u.lang = voz?.lang || 'es-CO';
        if (voz) u.voice = voz;
        u.rate = 1.05;
        if (i === 0) u.onstart = () => { if (turno.id === turnoActual) alHablar(true); };
        if (i === frases.length - 1) u.onend = u.onerror = () => { if (turno.id === turnoActual) alHablar(false); };
        speechSynthesis.speak(u);
      });
    },
    fallo(turno) { if (turno && turno.id === turnoActual) cortar(); },
  };
}

/** Micrófono con el reconocimiento de voz del navegador (Chrome/Edge/Safari). Devuelve null si no existe. */
export function crearMicrofono(alTexto: (t: string) => void, alEstado: (escuchando: boolean) => void): { alternar(): void } | null {
  const w = window as unknown as { SpeechRecognition?: new () => any; webkitSpeechRecognition?: new () => any };
  const Reconocedor = w.SpeechRecognition || w.webkitSpeechRecognition;
  if (!Reconocedor) return null;
  const rec = new Reconocedor();
  rec.lang = 'es-CO';
  rec.interimResults = false;
  rec.maxAlternatives = 1;
  let activo = false;
  rec.onresult = (e: any) => { const t = e.results?.[0]?.[0]?.transcript?.trim(); if (t) alTexto(t); };
  rec.onend = () => { activo = false; alEstado(false); };
  rec.onerror = () => { activo = false; alEstado(false); };
  return {
    alternar() {
      if (activo) { rec.stop(); return; }
      try { rec.start(); activo = true; alEstado(true); } catch { /* ya estaba escuchando */ }
    },
  };
}
