import {
  AfterViewInit, ChangeDetectionStrategy, Component, DestroyRef, ElementRef, NgZone, ViewChild, inject, signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { CatalogService } from '../../services/catalog.service';
import { BookingService } from '../../services/booking.service';
import { AccionesPaginaService } from '../../services/acciones-pagina.service';
import { ServiceItem } from '../../models/service.model';
import { Voz, crearMicrofono, crearVoz, vozDisponible } from './asistente-voz';
import { interpretar } from './asistente-ordenes';

interface Mensaje { rol: 'user' | 'assistant'; contenido: string; error?: boolean; pendiente?: boolean; enlace?: { url: string; etiqueta: string }; }

const MAX_CARACTERES = 500;
const MAX_HISTORIAL = 12;
const CLAVE_SESION = 'recovr_asistente';
const SALUDO = 'Hola, soy la asistente de RECOVR. Te ayudo con tratamientos, precios, terapeutas y turnos libres. También puedo llevarte a cualquier parte de la página. ¿Qué necesitas?';

/**
 * Asistente flotante (traída de Wimbledon): una carita animada que abre un chat con el agente de negocio.
 * - Las dudas las responde la IA del backend (/api/ia/chat) con los datos reales de RECOVR.
 * - Las órdenes cortas ("llévame a las cabinas", "reservar nuru", "WhatsApp") las resuelve la página sola.
 * - Voz y micrófono del navegador, si los hay. Los mensajes se pintan como texto (nunca HTML).
 */
@Component({
  selector: 'app-asistente',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './asistente.component.html',
  styleUrls: ['./asistente.component.scss']
})
export class AsistenteComponent implements AfterViewInit {
  @ViewChild('lista') private listaRef?: ElementRef<HTMLDivElement>;
  @ViewChild('caja') private cajaRef?: ElementRef<HTMLInputElement>;
  @ViewChild('boton', { static: true }) private botonRef!: ElementRef<HTMLButtonElement>;

  private api = inject(ApiService);
  private booking = inject(BookingService);
  private acciones = inject(AccionesPaginaService);
  private zone = inject(NgZone);
  private destroyRef = inject(DestroyRef);
  private sanitizer = inject(DomSanitizer);

  readonly maxCaracteres = MAX_CARACTERES;
  readonly abierto = signal(false);
  readonly mensajes = signal<Mensaje[]>([{ rol: 'assistant', contenido: SALUDO }]);
  readonly enviando = signal(false);
  readonly escuchando = signal(false);
  readonly altavozActivo = signal(true);
  readonly conVoz = vozDisponible();
  readonly caraSrc = signal<SafeResourceUrl | null>(null);
  texto = '';

  private historial: Array<{ rol: 'user' | 'assistant'; contenido: string }> = [];
  private servicios: ServiceItem[] = [];
  private voz: Voz | null = null;
  private microfono: { alternar(): void } | null = null;
  private marco?: HTMLIFrameElement;
  readonly conMicrofono = signal(false);

  constructor() {
    inject(CatalogService).getFeaturedServices().subscribe(s => (this.servicios = s));
    try {
      const guardado = JSON.parse(sessionStorage.getItem(CLAVE_SESION) || '[]');
      if (Array.isArray(guardado)) {
        this.historial = guardado.slice(-MAX_HISTORIAL);
        this.mensajes.update(m => [...m, ...this.historial.map(h => ({ ...h }))]);
      }
    } catch { /* sin almacenamiento */ }

    if (this.conVoz) this.voz = crearVoz(h => this.avatarHablando(h));
    this.microfono = typeof window !== 'undefined'
      ? crearMicrofono(
          t => this.zone.run(() => { this.altavozActivo.set(true); this.texto = t; this.enviar(); }),
          e => this.zone.run(() => this.escuchando.set(e)))
      : null;
    this.conMicrofono.set(!!this.microfono);
  }

  ngAfterViewInit(): void {
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    // La carita se anima en una mini página aparte (/cara/avatar.html) para no mezclar su WebGL con el sitio.
    // Se carga después de la página para no competir con lo importante; la foto fija queda debajo mientras tanto.
    const cargar = () => setTimeout(() => this.caraSrc.set(this.sanitizer.bypassSecurityTrustResourceUrl('/cara/avatar.html')), 600);
    if (document.readyState === 'complete') cargar();
    else addEventListener('load', cargar, { once: true });

    // El iframe no recibe el mouse (pointer-events: none): le pasamos la posición del cursor para que la mire.
    this.zone.runOutsideAngular(() => {
      let cuadro = 0;
      let ultimo: PointerEvent | null = null;
      const mandar = () => {
        cuadro = 0;
        this.marco ??= this.botonRef.nativeElement.querySelector('iframe') ?? undefined;
        if (!this.marco || !ultimo) return;
        const r = this.marco.getBoundingClientRect();
        this.marco.contentWindow?.postMessage({ t: 'p', x: ultimo.clientX - r.left, y: ultimo.clientY - r.top }, location.origin);
      };
      const mover = (e: PointerEvent) => { ultimo = e; if (!cuadro) cuadro = requestAnimationFrame(mandar); };
      addEventListener('pointermove', mover, { passive: true });
      this.destroyRef.onDestroy(() => removeEventListener('pointermove', mover));
    });
  }

  alternar(abrir?: boolean): void {
    const valor = abrir ?? !this.abierto();
    this.abierto.set(valor);
    if (valor) setTimeout(() => { this.cajaRef?.nativeElement.focus(); this.bajar(); });
  }

  alEscape(): void {
    if (this.abierto()) this.alternar(false);
  }

  alternarAltavoz(): void {
    this.altavozActivo.update(v => !v);
    if (!this.altavozActivo()) this.voz?.cortar();
  }

  alternarMicrofono(): void {
    this.voz?.cortar();
    this.microfono?.alternar();
  }

  enviar(): void {
    const texto = this.texto.trim().slice(0, MAX_CARACTERES);
    if (!texto || this.enviando()) return;
    this.texto = '';

    const orden = interpretar(texto, this.servicios);
    if (orden) {
      this.agregar({ rol: 'user', contenido: texto });
      this.agregar({ rol: 'assistant', contenido: orden.texto, enlace: orden.tipo === 'enlace' ? { url: orden.url, etiqueta: orden.etiqueta } : undefined });
      switch (orden.tipo) {
        case 'seccion': this.acciones.irASeccion(orden.id); break;
        case 'ver': this.acciones.abrirTratamiento(orden.servicio); break;
        case 'reservar': this.booking.navigateToBooking(orden.servicio); break;
        case 'ruta': this.acciones.irA(orden.ruta); break;
        case 'enlace': break;
      }
      if (this.voz && this.altavozActivo()) this.voz.decir(orden.texto, this.voz.empezar());
      return;
    }

    this.agregar({ rol: 'user', contenido: texto });
    this.historial = [...this.historial, { rol: 'user' as const, contenido: texto }].slice(-MAX_HISTORIAL);
    // La conversación que se manda debe empezar en un mensaje del usuario
    while (this.historial.length && this.historial[0].rol !== 'user') this.historial.shift();

    const pendiente: Mensaje = { rol: 'assistant', contenido: 'Escribiendo…', pendiente: true };
    this.agregar(pendiente);
    this.enviando.set(true);
    const turno = this.voz && this.altavozActivo() ? this.voz.empezar() : null;

    this.api.chatIa(this.historial).subscribe({
      next: respuesta => {
        this.reemplazar(pendiente, { rol: 'assistant', contenido: respuesta });
        this.historial = [...this.historial, { rol: 'assistant' as const, contenido: respuesta }].slice(-MAX_HISTORIAL);
        if (turno) this.voz?.decir(respuesta, turno);
        this.terminar();
      },
      error: (err: unknown) => {
        const msg = err instanceof Error && err.message ? err.message : 'No pude responder ahora. Intenta de nuevo.';
        this.reemplazar(pendiente, { rol: 'assistant', contenido: msg, error: true });
        this.historial = this.historial.slice(0, -1); // el mensaje sin respuesta no se reenvía
        if (turno) this.voz?.fallo(turno);
        this.terminar();
      }
    });
  }

  private terminar(): void {
    this.enviando.set(false);
    try { sessionStorage.setItem(CLAVE_SESION, JSON.stringify(this.historial)); } catch { /* sin almacenamiento */ }
    setTimeout(() => this.cajaRef?.nativeElement.focus());
  }

  private agregar(m: Mensaje): void {
    this.mensajes.update(lista => [...lista, m]);
    this.bajar();
  }

  private reemplazar(viejo: Mensaje, nuevo: Mensaje): void {
    this.mensajes.update(lista => lista.map(m => (m === viejo ? nuevo : m)));
    this.bajar();
  }

  private bajar(): void {
    setTimeout(() => {
      const el = this.listaRef?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }

  private avatarHablando(hablando: boolean): void {
    this.marco ??= this.botonRef.nativeElement.querySelector('iframe') ?? undefined;
    this.marco?.contentWindow?.postMessage({ t: 'voz', hablando }, location.origin);
  }
}
