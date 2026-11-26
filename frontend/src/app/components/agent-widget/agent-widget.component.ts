import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { timeout } from 'rxjs/operators';

type RolMensaje = 'bot' | 'user';
type AccionMensaje = 'reserva' | 'whatsapp' | 'catalogo';

interface Propuesta {
  ticket: string;
  resumen: string;
}

interface Mensaje {
  rol: RolMensaje;
  texto: string;
  accion?: AccionMensaje;
  propuesta?: Propuesta;
  confirmada?: boolean;
}

interface Servicio {
  codigo: string;
  nombre: string;
  precio: number;
  minutos: number;
  claves: string[];
}

interface Terapeuta {
  nombre: string;
  especialidad: string;
}

/**
 * Concierge con voz de RECOVR.
 * - Escucha con SpeechRecognition (dictado en español).
 * - Responde por texto y voz con SpeechSynthesis.
 * - Conoce el catálogo, precios, horario, cabinas, terapeutas y políticas del spa.
 * Los datos salen del catálogo real cargado en la base (backend/CREDENCIALES_DEMO.md y tabla `servicios`).
 */
@Component({
  selector: 'app-agent-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './agent-widget.component.html',
  styleUrls: ['./agent-widget.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgentWidgetComponent {
  private router = inject(Router);
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);

  @ViewChild('entrada') entrada?: ElementRef<HTMLInputElement>;
  @ViewChild('registro') registro?: ElementRef<HTMLElement>;
  @ViewChild('caraFrame') caraFrame?: ElementRef<HTMLIFrameElement>;

  readonly abierto = signal(false);
  readonly escuchando = signal(false);
  readonly hablando = signal(false);
  readonly escribiendo = signal(false);
  readonly confirmando = signal('');
  readonly dictado = signal('');
  readonly mensajes = signal<Mensaje[]>([
    {
      rol: 'bot',
      texto:
        'Hola, soy Aura, la concierge de RECOVR. Te cuento servicios y precios, horarios, terapeutas o te ayudo a reservar. También puedes hablarme con el micrófono.',
    },
  ]);

  readonly sugerencias = ['Precios', 'Horario', 'Reservar', 'Terapeutas'];

  readonly puedeEscuchar = computed(() => this.soporteDictado);

  private readonly SERVICIOS: Servicio[] = [
    {
      codigo: 'RC-01',
      nombre: 'Masaje Tántrico Sensitivo',
      precio: 200,
      minutos: 60,
      claves: ['tantrico sensitivo', 'sensitivo', 'tantrico'],
    },
    {
      codigo: 'RC-02',
      nombre: 'Masaje Nuru Body Slide',
      precio: 280,
      minutos: 75,
      claves: ['nuru', 'body slide', 'body'],
    },
    {
      codigo: 'RC-03',
      nombre: 'Masaje Relajante & Descontracturante',
      precio: 180,
      minutos: 60,
      claves: ['relajante', 'descontracturante', 'relajacion'],
    },
    {
      codigo: 'RC-04',
      nombre: 'Masaje Tántrico Completo',
      precio: 250,
      minutos: 75,
      claves: ['tantrico completo', 'completo'],
    },
    {
      codigo: 'RC-05',
      nombre: 'Masaje a Cuatro Manos',
      precio: 380,
      minutos: 60,
      claves: ['cuatro manos', '4 manos', 'cuatro'],
    },
    {
      codigo: 'RC-06',
      nombre: 'RECOVR VIP Hidromasaje',
      precio: 450,
      minutos: 90,
      claves: ['vip', 'hidromasaje', 'microburbujas'],
    },
  ];

  private readonly TERAPEUTAS: Terapeuta[] = [
    { nombre: 'Ely', especialidad: 'Masaje Tántrico Sensitivo' },
    { nombre: 'Miranda', especialidad: 'Masaje Nuru Body Slide' },
    { nombre: 'Pamela', especialidad: 'Descontracturante y Cuatro Manos' },
    { nombre: 'Maria', especialidad: 'Suite Presencial e Hidromasaje' },
  ];

  private readonly CABINAS = [
    'Cabina 01 Néon Rose',
    'Cabina 02 Nuru Suite',
    'Cabina 03 Dúo / Cuatro Manos',
    'Cabina 04 VIP Hidromasaje',
  ];

  private readonly soporteDictado: boolean =
    typeof window !== 'undefined' &&
    !!((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition);

  private reconocimiento: any = null;
  private vozEspanol?: SpeechSynthesisVoice;
  private usuarioHablo = false;

  constructor() {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      this.cargarVoces();
      const synth = window.speechSynthesis;
      synth.onvoiceschanged = () => this.cargarVoces();
    }
    this.destroyRef.onDestroy(() => this.detenerTodo());
  }

  // ---------------------------------------------------------------- UI

  alternar(): void {
    const nuevo = !this.abierto();
    this.abierto.set(nuevo);
    if (nuevo) {
      setTimeout(() => this.entrada?.nativeElement.focus(), 120);
    } else {
      this.detenerHabla();
      this.detenerEscucha();
    }
  }

  cerrar(): void {
    this.abierto.set(false);
    this.detenerHabla();
    this.detenerEscucha();
  }

  enviarDesdeCaja(input: HTMLInputElement): void {
    const texto = input.value.trim();
    if (!texto) return;
    input.value = '';
    this.responder(texto, true);
  }

  usarSugerencia(texto: string): void {
    this.responder(texto, true);
  }

  ejecutarAccion(accion: AccionMensaje): void {
    if (accion === 'reserva') {
      this.cerrar();
      this.router.navigate(['/reserva']);
      return;
    }
    if (accion === 'catalogo') {
      this.cerrar();
      this.router.navigate(['/catalog']);
      return;
    }
    if (accion === 'whatsapp') {
      window.open('https://wa.me/51924040992?text=Hola%20RECOVR%2C%20quiero%20informaci%C3%B3n%20sobre%20una%20reserva.', '_blank');
    }
  }

  // ---------------------------------------------------------------- Voz

  alternarEscucha(): void {
    if (this.escuchando()) {
      this.detenerEscucha();
      return;
    }
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      this.agregarMensaje({
        rol: 'bot',
        texto:
          'Tu navegador no soporta el dictado por voz. Puedes escribirme aquí o usar Chrome, Edge o Safari.',
      });
      return;
    }
    this.detenerHabla();

    const r = new SpeechRecognition();
    r.lang = 'es-ES';
    r.interimResults = true;
    r.continuous = false;
    r.maxAlternatives = 1;

    let final = '';
    r.onstart = () => {
      this.escuchando.set(true);
      this.dictado.set('');
    };
    r.onresult = (evento: any) => {
      let interino = '';
      for (let i = evento.resultIndex; i < evento.results.length; i++) {
        const parte = evento.results[i][0].transcript;
        if (evento.results[i].isFinal) final += parte;
        else interino += parte;
      }
      this.dictado.set((final + ' ' + interino).trim());
    };
    r.onerror = (evento: any) => {
      this.escuchando.set(false);
      this.dictado.set('');
      if (evento?.error === 'not-allowed' || evento?.error === 'service-not-allowed') {
        this.agregarMensaje({
          rol: 'bot',
          texto: 'Necesito permiso para usar el micrófono. Actívalo en el candado del navegador y vuelve a intentarlo.',
        });
      } else if (evento?.error !== 'aborted' && evento?.error !== 'no-speech') {
        this.agregarMensaje({
          rol: 'bot',
          texto: 'No pude escucharte bien. Inténtalo de nuevo o escríbeme.',
        });
      }
    };
    r.onend = () => {
      this.escuchando.set(false);
      const texto = final.trim();
      this.dictado.set('');
      if (texto) this.responder(texto, true);
    };

    this.reconocimiento = r;
    try {
      r.start();
    } catch {
      this.escuchando.set(false);
    }
  }

  private detenerEscucha(): void {
    try {
      this.reconocimiento?.stop();
    } catch {
      /* ya detenido */
    }
    this.reconocimiento = null;
    this.escuchando.set(false);
    this.dictado.set('');
  }

  private cargarVoces(): void {
    const synth = window.speechSynthesis;
    const voces = synth.getVoices();
    if (!voces.length) return;
    const espanol = voces.filter((v) => /^es/i.test(v.lang));
    if (!espanol.length) return;
    const femenina = /(camila|rosa|monica|mónica|paulina|sabina|helena|lucia|lucía|female|mujer|google)/i;
    this.vozEspanol =
      espanol.find((v) => /es[-_]PE/i.test(v.lang) && femenina.test(v.name)) ??
      espanol.find((v) => /es[-_]PE/i.test(v.lang)) ??
      espanol.find((v) => /(419|MX|US)/i.test(v.lang) && femenina.test(v.name)) ??
      espanol.find((v) => femenina.test(v.name)) ??
      espanol[0];
  }

  private hablar(texto: string): void {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    const limpio = this.limpiarParaVoz(texto);
    if (!limpio) return;
    const synth = window.speechSynthesis;
    synth.cancel();

    const frases = limpio.match(/[^.!?]+[.!?]?/g) ?? [limpio];
    frases.forEach((frase, indice) => {
      const u = new SpeechSynthesisUtterance(frase.trim());
      u.lang = 'es-PE';
      u.rate = 1;
      u.pitch = 1.05;
      if (this.vozEspanol) u.voice = this.vozEspanol;
      if (indice === 0) u.onstart = () => {
        this.hablando.set(true);
        this.avisarCara(true);
      };
      if (indice === frases.length - 1) {
        u.onend = () => {
          this.hablando.set(false);
          this.avisarCara(false);
        };
        u.onerror = () => {
          this.hablando.set(false);
          this.avisarCara(false);
        };
      }
      synth.speak(u);
    });
  }

  private detenerHabla(): void {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    this.hablando.set(false);
    this.avisarCara(false);
  }

  /** Avisa al avatar WebGL para que mueva la boca mientras suena la voz. */
  conectarCara(): void {
    if (this.hablando()) this.avisarCara(true);
  }

  private avisarCara(activo: boolean): void {
    try {
      this.caraFrame?.nativeElement.contentWindow?.postMessage({ t: 'voz', hablando: activo }, window.location.origin);
    } catch {
      /* el iframe todavía no está listo */
    }
  }

  private limpiarParaPantalla(texto: string): string {
    return String(texto || '')
      .replace(/\*\*(.+?)\*\*/g, '$1')
      .replace(/\*(.+?)\*/g, '$1')
      .replace(/^\s*[-*]\s+/gm, '• ')
      .replace(/#{1,6}\s*/g, '')
      .trim();
  }

  private limpiarParaVoz(texto: string): string {
    return texto
      .replace(/[*_#`]/g, '')
      .replace(/[•·]/g, '. ')
      .replace(/[—–]/g, ', ')
      .replace(/S\/\s?/g, '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  private detenerTodo(): void {
    this.detenerHabla();
    this.detenerEscucha();
  }

  // ---------------------------------------------------------------- Motor de respuestas

  private responder(texto: string, hablar: boolean): void {
    this.usuarioHablo = true;
    this.agregarMensaje({ rol: 'user', texto });
    this.escribiendo.set(true);
    void this.consultarAgente(texto, hablar);
  }

  private async consultarAgente(texto: string, hablar: boolean): Promise<void> {
    const historial = this.mensajes()
      .filter((m) => m.rol === 'user' || m.rol === 'bot')
      .map((m) => ({ rol: m.rol, texto: m.texto }))
      .slice(-12);

    try {
      const respuesta = await firstValueFrom(
        this.http
          .post<{ respuesta: string; propuesta?: Propuesta }>('/agente/chat', { mensajes: historial })
          .pipe(timeout(50000)),
      );
      this.escribiendo.set(false);
      const mensaje: Mensaje = { rol: 'bot', texto: this.limpiarParaPantalla(respuesta.respuesta) || '¿Me repites eso, por favor?' };
      if (respuesta.propuesta) mensaje.propuesta = respuesta.propuesta;
      this.agregarMensaje(mensaje);
      if (hablar) this.hablar(mensaje.texto);
    } catch {
      // Sin agente (red, cuota o servicio caído): responde el motor local con los datos del spa.
      this.escribiendo.set(false);
      const respaldo = this.pensar(texto);
      this.agregarMensaje(respaldo);
      if (hablar) this.hablar(respaldo.texto);
    }
  }

  async confirmar(ticket: string, indice: number): Promise<void> {
    if (this.confirmando()) return;
    this.confirmando.set(ticket);
    try {
      const r = await firstValueFrom(
        this.http
          .post<{ mensaje?: string }>('/agente/confirmar', { ticket })
          .pipe(timeout(30000)),
      );
      const texto = r.mensaje || 'Reserva confirmada.';
      this.mensajes.update((lista) => lista.map((m, i) => (i === indice ? { ...m, confirmada: true } : m)));
      this.agregarMensaje({ rol: 'bot', texto });
      this.hablar(texto);
    } catch (error: any) {
      const texto = error?.error?.error || 'No pude confirmar la reserva. ¿Lo intentamos otra vez?';
      this.mensajes.update((lista) => lista.map((m, i) => (i === indice ? { ...m, confirmada: true } : m)));
      this.agregarMensaje({ rol: 'bot', texto });
      this.hablar(texto);
    } finally {
      this.confirmando.set('');
    }
  }

  private pensar(entrada: string): Mensaje {
    const t = this.normalizar(entrada);
    const tiene = (...claves: string[]) => claves.some((c) => t.includes(c));

    // Servicio concreto por nombre
    const servicio = this.SERVICIOS.find((s) => s.claves.some((c) => t.includes(c)));
    if (servicio && (tiene('precio', 'cuesta', 'cuanto', 'tarifa', 'sale', 'vale', 'incluye', 'duracion', 'dura') || t.length < 40)) {
      return {
        rol: 'bot',
        texto:
          `${servicio.nombre} (${servicio.codigo}) cuesta S/ ${servicio.precio} e incluye ${servicio.minutos} minutos de sesión. ` +
          'Se realiza en cabina privada climatizada, con ducha y atención personalizada. ¿Quieres que te lleve a reservarlo?',
        accion: 'reserva',
      };
    }

    if (tiene('hola', 'buenas', 'buenos dias', 'buenas tardes', 'buenas noches', 'hey', 'que tal')) {
      return {
        rol: 'bot',
        texto:
          'Hola, qué gusto tenerte aquí. Puedo contarte los masajes y precios, el horario, las terapeutas o ayudarte a reservar en menos de un minuto.',
      };
    }

    if (tiene('precio', 'cuesta', 'cuanto', 'tarifa', 'catalogo', 'servicio', 'masaje', 'ritual', 'opciones')) {
      const lista = this.SERVICIOS.map(
        (s) => `• ${s.nombre} (${s.codigo}) — S/ ${s.precio} · ${s.minutos} min`,
      ).join('\n');
      return {
        rol: 'bot',
        texto:
          `Estos son los rituales disponibles:\n${lista}\n\n` +
          'Todos se realizan en cabina privada climatizada, con ducha y atención personalizada.',
        accion: 'reserva',
      };
    }

    if (tiene('horario', 'hora', 'abren', 'cierran', 'atienden', 'abierto', 'cerrado', 'ultima reserva')) {
      return {
        rol: 'bot',
        texto:
          'Las reservas se atienden de 12:00 a 23:00. La sesión completa debe terminar antes del cierre, así que la última reserva depende de la duración del masaje. Al reservar ves los turnos reales disponibles.',
        accion: 'reserva',
      };
    }

    if (tiene('donde', 'ubicacion', 'direccion', 'local', 'llego', 'surco', 'lima', 'como llegar')) {
      return {
        rol: 'bot',
        texto:
          'Estamos en Av. Primavera 1250, Santiago de Surco, Lima. El local es discreto, con recepción privada y cabinas individuales climatizadas.',
      };
    }

    if (tiene('terapeuta', 'masajista', 'especialista', 'quien', 'ely', 'miranda', 'pamela', 'maria', 'chica')) {
      const lista = this.TERAPEUTAS.map((x) => `• ${x.nombre} — ${x.especialidad}`).join('\n');
      return {
        rol: 'bot',
        texto:
          `Nuestras especialistas son:\n${lista}\n\n` +
          'Puedes elegir a tu terapeuta al reservar y ver su disponibilidad real por fecha y turno.',
        accion: 'reserva',
      };
    }

    if (tiene('reservar', 'reserva', 'cita', 'agendar', 'turno', 'disponibilidad', 'separar')) {
      return {
        rol: 'bot',
        texto:
          'Te llevo al asistente de reserva: eliges el masaje, la terapeuta, la fecha y un turno con disponibilidad real. Al confirmar, el pago es completo y el cupo queda retenido 15 minutos.',
        accion: 'reserva',
      };
    }

    if (tiene('pago', 'pagar', 'tarjeta', 'efectivo', 'yape', 'plin', 'deposito', 'transferencia', 'cuotas')) {
      return {
        rol: 'bot',
        texto:
          'El pago es completo al confirmar la cita. La reserva queda pendiente 15 minutos mientras se completa el pago; si vence, el turno se libera. En recepción te indican los medios disponibles.',
      };
    }

    if (tiene('cancelar', 'cancelacion', 'anular', 'reprogramar', 'cambiar la cita', 'devolucion')) {
      return {
        rol: 'bot',
        texto:
          'Puedes cancelar hasta 4 horas antes de tu cita y un máximo de 2 veces por mes. Si necesitas reprogramar, lo coordinamos por WhatsApp o con recepción.',
        accion: 'whatsapp',
      };
    }

    if (tiene('privacidad', 'discreto', 'discrecion', 'confidencial', 'secreto', 'reservado')) {
      return {
        rol: 'bot',
        texto:
          'La privacidad es parte de la experiencia: cabinas individuales climatizadas, recepción discreta y atención personalizada. Tus datos y tu ficha se tratan de forma confidencial.',
      };
    }

    if (tiene('cabina', 'sala', 'suite', 'hidromasaje', 'microburbujas', 'ducha')) {
      return {
        rol: 'bot',
        texto:
          `Contamos con ${this.CABINAS.length} cabinas privadas: ${this.CABINAS.join(', ')}. ` +
          'La suite VIP incluye hidromasaje y microburbujas.',
        accion: 'reserva',
      };
    }

    if (tiene('whatsapp', 'contacto', 'telefono', 'numero', 'correo', 'escribir', 'hablar con alguien', 'recepcion', 'humano', 'persona')) {
      return {
        rol: 'bot',
        texto:
          'Puedes escribirnos al WhatsApp +51 924 040 992. Recepción responde en horario de atención, de 12:00 a 23:00.',
        accion: 'whatsapp',
      };
    }

    if (tiene('gracias', 'perfecto', 'genial', 'buenisimo', 'ok', 'listo')) {
      return {
        rol: 'bot',
        texto: 'Con gusto. Si quieres, te dejo en la reserva y eliges tu turno ahora mismo.',
        accion: 'reserva',
      };
    }

    return {
      rol: 'bot',
      texto:
        'No tengo ese dato a mano. Puedo ayudarte con servicios y precios, horario, terapeutas, ubicación, cabinas, pagos o reservas. ' +
        'Si prefieres hablar con una persona, escríbenos al WhatsApp +51 924 040 992.',
      accion: 'whatsapp',
    };
  }

  private agregarMensaje(mensaje: Mensaje): void {
    this.mensajes.update((lista) => [...lista, mensaje]);
    setTimeout(() => {
      const el = this.registro?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    }, 30);
  }

  private normalizar(texto: string): string {
    return texto
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[¿?¡!.,;:()"'“”]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }
}
