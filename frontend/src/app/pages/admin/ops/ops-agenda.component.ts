import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../services/admin-api.service';
import { ApiService } from '../../../services/api.service';
import {
  ClienteOps, EmpleadoEstado, ReservaForm, ReservaOps, SalaEstado, ServicioOps
} from '../../../models/ops.model';
import { OpsGateComponent } from './ops-gate.component';
import { OpsCobroModalComponent } from './ops-cobro-modal.component';
import { ESTADO_TXT, codigoReserva, fechaHoraTxt, fechaInput, horaTxt, soles } from './ops.util';

type Rango = 'HOY' | 'MANANA' | 'SEMANA' | 'PASADAS' | 'TODAS';

/**
 * Agenda y control de salas: CRUD completo de reservas sobre la API.
 * Flujo: En espera → (cobrar) Confirmada → (check-in) En sesión → (finalizar o fin de sesión) Completada.
 * Cancelar es posible mientras la reserva esté activa; solo se elimina una reserva ya cancelada (solo ADMIN).
 */
@Component({
  selector: 'app-ops-agenda',
  standalone: true,
  imports: [FormsModule, OpsGateComponent, OpsCobroModalComponent],
  styleUrl: './ops.scss',
  template: `
    <app-ops-gate (connected)="cargar()" />
    @if (api.session()) {
      @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }

      <div class="legend">
        <span><span class="badge EN_ESPERA">En espera</span> espera el cobro; si no se confirma a los 15 min del inicio, expira</span>
        <span><span class="badge CONFIRMADA">Confirmada</span> pagada; check-in desde 30 min antes; sin llegar en 30 min = no asistió</span>
        <span><span class="badge EN_SESION">En sesión</span> con check-in</span>
        <span><span class="badge COMPLETADA">Completada</span> al terminar la sesión</span>
      </div>

      <div class="ops-head">
        <div class="ops-row">
          @for (r of rangos; track r.key) {
            <button type="button" class="ops-chip" [class.on]="rango() === r.key" (click)="rango.set(r.key)">{{ r.label }}</button>
          }
        </div>
        <div class="ops-row">
          <select class="ops-select" aria-label="Filtrar por estado" [ngModel]="estadoFiltro()" (ngModelChange)="estadoFiltro.set($event)">
            <option value="">Todos los estados</option>
            @for (e of estados; track e.key) { <option [value]="e.key">{{ e.label }}</option> }
          </select>
          <select class="ops-select" aria-label="Filtrar por cabina" [ngModel]="salaFiltro()" (ngModelChange)="salaFiltro.set($event)">
            <option value="">Todas las cabinas</option>
            @for (s of salas(); track s.id) { <option [value]="s.id">{{ s.nombre }}</option> }
          </select>
          <input class="ops-input" type="search" placeholder="Buscar cliente o servicio" aria-label="Buscar" [ngModel]="texto()" (ngModelChange)="texto.set($event)">
          <button type="button" class="ops-btn" (click)="cargar()">↻ Actualizar</button>
          @if (!soloLectura()) { <button type="button" class="ops-btn primary" (click)="abrirNueva()">+ Nueva reserva</button> }
        </div>
      </div>

      <div class="ops-table-wrap">
        <table class="ops-table">
          <thead>
            <tr><th>Código</th><th>Cliente</th><th>Servicio</th><th>Terapeuta</th><th>Cabina</th><th>Horario</th><th>Pago</th><th>Estado</th><th>Acciones</th></tr>
          </thead>
          <tbody>
            @for (r of visibles(); track r.id) {
              <tr>
                <td class="mono">{{ codigo(r.id) }}</td>
                <td><strong>{{ r.clienteNombre }}</strong></td>
                <td>{{ r.servicioNombre }}</td>
                <td>{{ r.empleadoNombre }}</td>
                <td>{{ r.salaNombre }}</td>
                <td>{{ fechaHora(r.fechaHora) }}<div class="muted">hasta {{ hora(r.finBloque) }} (incl. limpieza)</div></td>
                <td>
                  @if (r.pagado) { <span class="badge pagado">Pagado</span><div class="muted">{{ r.metodoPago }} · {{ soles(r.precio) }}</div> }
                  @else { <span class="badge pendiente">Pendiente</span><div class="muted">{{ soles(r.precio) }}</div> }
                </td>
                <td>
                  <span class="badge" [class]="'badge ' + r.estadoOperativo">{{ estadoTxt[r.estadoOperativo] }}</span>
                  @if (r.observacion) { <div class="muted" [title]="r.observacion">{{ r.observacion }}</div> }
                </td>
                <td>
                  <div class="ops-actions">
                    @if (soloLectura()) { <span class="muted">Solo lectura</span> }
                    @else { @switch (r.estadoOperativo) {
                      @case ('EN_ESPERA') {
                        <button type="button" class="ops-btn sm ok" (click)="cobrando.set(r)">Cobrar y confirmar</button>
                        <button type="button" class="ops-btn sm" (click)="abrirEditar(r)">Editar</button>
                        <button type="button" class="ops-btn sm danger" (click)="cancelar(r)">Cancelar</button>
                      }
                      @case ('CONFIRMADA') {
                        <button type="button" class="ops-btn sm info" (click)="accion(admin.checkIn(r.id), 'Check-in registrado.')">Check-in</button>
                        <button type="button" class="ops-btn sm" (click)="abrirEditar(r)">Editar</button>
                        <button type="button" class="ops-btn sm danger" (click)="cancelar(r)">Cancelar</button>
                      }
                      @case ('EN_SESION') {
                        <button type="button" class="ops-btn sm ok" (click)="accion(admin.finalizar(r.id), 'Sesión finalizada.')">Finalizar</button>
                        <button type="button" class="ops-btn sm danger" (click)="cancelar(r)">Cancelar</button>
                      }
                      @case ('CANCELADA') {
                        @if (esAdmin()) {
                          <button type="button" class="ops-btn sm danger" (click)="eliminar(r)">Eliminar</button>
                        } @else { <span class="muted">Solo ADMIN elimina</span> }
                      }
                      @default { <span class="muted">Historial</span> }
                    } }
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="9" class="ops-empty">No hay reservas para este filtro.</td></tr>
            }
          </tbody>
        </table>
      </div>
      <p class="ops-sub" style="margin-top:0.6rem">{{ visibles().length }} de {{ reservas().length }} reservas · se actualiza solo cada 30 s</p>

      @if (cobrando(); as c) {
        <app-ops-cobro-modal [reserva]="c" (cerrar)="cobrando.set(null)" (cobrada)="cobrando.set(null); aviso('Pago registrado: la reserva quedó CONFIRMADA.'); cargar()" />
      }

      @if (form(); as f) {
        <div class="modal-back" (click)="form.set(null)">
          <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="guardar()">
            <h3>{{ editandoId() ? 'Editar reserva ' + codigo(editandoId()!) : 'Nueva reserva' }}</h3>
            @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
            <div class="form-grid">
              <div class="field full">
                <label for="rv-cliente">Cliente</label>
                <select id="rv-cliente" class="ops-select" name="cliente" [(ngModel)]="f.clienteId" required>
                  <option [ngValue]="null" disabled>Selecciona un cliente</option>
                  @for (c of clientes(); track c.id) { <option [ngValue]="c.id">{{ c.nombre }} · {{ c.correo }}</option> }
                </select>
              </div>
              <div class="field full">
                <label for="rv-servicio">Servicio</label>
                <select id="rv-servicio" class="ops-select" name="servicio" [(ngModel)]="f.servicioId" required>
                  <option [ngValue]="null" disabled>Selecciona un servicio</option>
                  @for (s of servicios(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }} · {{ s.duracionMinutos }} min · {{ soles(s.precio) }}</option> }
                </select>
              </div>
              <div class="field">
                <label for="rv-terapeuta">Terapeuta</label>
                <select id="rv-terapeuta" class="ops-select" name="terapeuta" [(ngModel)]="f.empleadoId" required>
                  <option [ngValue]="null" disabled>Selecciona</option>
                  @for (e of empleados(); track e.id) { <option [ngValue]="e.id">{{ e.nombre }}</option> }
                </select>
              </div>
              <div class="field">
                <label for="rv-cabina">Cabina</label>
                <select id="rv-cabina" class="ops-select" name="cabina" [(ngModel)]="f.salaId">
                  <option [ngValue]="null">Automática (primera libre)</option>
                  @for (s of salas(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }}</option> }
                </select>
              </div>
              <div class="field">
                <label for="rv-fecha">Fecha</label>
                <input id="rv-fecha" class="ops-input" type="date" name="fecha" [(ngModel)]="f.fecha" [min]="hoy" required>
              </div>
              <div class="field">
                <label for="rv-hora">Hora</label>
                <input id="rv-hora" class="ops-input" type="time" name="hora" step="900" [(ngModel)]="f.hora" required>
              </div>
            </div>
            <p class="ops-sub" style="margin-top:0.8rem">El sistema rechaza (409) si la cabina o la terapeuta ya tienen otra reserva activa que se cruce, contando duración + limpieza.</p>
            <div class="modal-foot">
              <button type="button" class="ops-btn" (click)="form.set(null)">Volver</button>
              <button type="submit" class="ops-btn primary" [disabled]="enviando()">{{ enviando() ? 'Guardando…' : 'Guardar' }}</button>
            </div>
          </form>
        </div>
      }
    }
  `
})
export class OpsAgendaComponent implements OnInit, OnDestroy {
  admin = inject(AdminApiService);
  api = inject(ApiService);

  reservas = signal<ReservaOps[]>([]);
  clientes = signal<ClienteOps[]>([]);
  servicios = signal<ServicioOps[]>([]);
  empleados = signal<EmpleadoEstado[]>([]);
  salas = signal<SalaEstado[]>([]);

  rango = signal<Rango>('HOY');
  estadoFiltro = signal('');
  salaFiltro = signal('');
  texto = signal('');
  mensaje = signal<{ texto: string; error: boolean } | null>(null);

  cobrando = signal<ReservaOps | null>(null);
  form = signal<ReservaForm | null>(null);
  editandoId = signal<number | null>(null);
  errorForm = signal('');
  enviando = signal(false);

  readonly estadoTxt = ESTADO_TXT;
  readonly rangos: { key: Rango; label: string }[] = [
    { key: 'HOY', label: 'Hoy' }, { key: 'MANANA', label: 'Mañana' }, { key: 'SEMANA', label: 'Próximos 7 días' },
    { key: 'PASADAS', label: 'Pasadas' }, { key: 'TODAS', label: 'Todas' }
  ];
  readonly estados = (Object.keys(ESTADO_TXT) as (keyof typeof ESTADO_TXT)[]).map(k => ({ key: k, label: ESTADO_TXT[k] }));
  readonly hoy = fechaInput();
  codigo = codigoReserva; hora = horaTxt; fechaHora = fechaHoraTxt; soles = soles;

  esAdmin = computed(() => this.api.session()?.role === 'ADMIN');
  /** Una cuenta ESPECIALISTA solo ve su propia agenda futura, sin acciones. */
  soloLectura = computed(() => this.api.session()?.role === 'ESPECIALISTA');

  visibles = computed(() => {
    const ahora = new Date();
    const inicioHoy = new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate());
    const dia = 86_400_000;
    const q = this.texto().trim().toLowerCase();
    const lista = this.reservas().filter(r => {
      const t = new Date(r.fechaHora).getTime();
      const d = t - inicioHoy.getTime();
      const enRango = {
        HOY: d >= 0 && d < dia, MANANA: d >= dia && d < 2 * dia, SEMANA: d >= 0 && d < 7 * dia,
        PASADAS: d < 0, TODAS: true
      }[this.rango()];
      return enRango
        && (!this.estadoFiltro() || r.estadoOperativo === this.estadoFiltro())
        && (!this.salaFiltro() || String(r.salaId) === this.salaFiltro())
        && (!q || r.clienteNombre.toLowerCase().includes(q) || r.servicioNombre.toLowerCase().includes(q));
    });
    const asc = this.rango() !== 'PASADAS' && this.rango() !== 'TODAS';
    return lista.sort((a, b) => (asc ? 1 : -1) * a.fechaHora.localeCompare(b.fechaHora));
  });

  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    if (this.api.session()) { this.cargar(); }
    this.timer = setInterval(() => { if (this.api.session() && !this.form() && !this.cobrando()) { this.cargar(true); } }, 30_000);
  }

  ngOnDestroy(): void {
    if (this.timer) { clearInterval(this.timer); }
  }

  aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(silencioso = false): void {
    if (this.soloLectura()) {
      this.admin.miAgenda().subscribe({
        next: r => { this.reservas.set(r); this.rango.set('SEMANA'); this.mensaje.set(null); },
        error: e => { if (!silencioso) { this.aviso(e.message, true); } }
      });
      return;
    }
    forkJoin({
      reservas: this.admin.reservas(), clientes: this.admin.clientes(), servicios: this.admin.servicios(),
      empleados: this.admin.estadoEmpleados(), salas: this.admin.estadoSalas()
    }).subscribe({
      next: d => {
        this.reservas.set(d.reservas); this.clientes.set(d.clientes); this.servicios.set(d.servicios);
        this.empleados.set(d.empleados); this.salas.set(d.salas);
        if (!silencioso) { this.mensaje.set(null); }
      },
      error: e => { if (!silencioso) { this.aviso(e.message, true); } }
    });
  }

  accion(obs: ReturnType<AdminApiService['checkIn']>, ok: string): void {
    obs.subscribe({ next: () => { this.aviso(ok); this.cargar(true); }, error: e => this.aviso(e.message, true) });
  }

  cancelar(r: ReservaOps): void {
    if (confirm(`¿Cancelar la reserva ${codigoReserva(r.id)} de ${r.clienteNombre}? Se libera el horario y se conserva el historial.`)) {
      this.accion(this.admin.cancelar(r.id), 'Reserva cancelada.');
    }
  }

  eliminar(r: ReservaOps): void {
    if (confirm(`¿Eliminar definitivamente la reserva ${codigoReserva(r.id)}? Esta acción no se puede deshacer.`)) {
      this.admin.eliminarReserva(r.id).subscribe({
        next: () => { this.aviso('Reserva eliminada.'); this.cargar(true); },
        error: e => this.aviso(e.message, true)
      });
    }
  }

  abrirNueva(): void {
    this.editandoId.set(null);
    this.errorForm.set('');
    this.form.set({ clienteId: null, empleadoId: null, servicioId: null, salaId: null, fecha: fechaInput(), hora: '16:00' });
  }

  abrirEditar(r: ReservaOps): void {
    this.editandoId.set(r.id);
    this.errorForm.set('');
    this.form.set({
      clienteId: r.clienteId, empleadoId: r.empleadoId, servicioId: r.servicioId, salaId: r.salaId,
      fecha: r.fechaHora.substring(0, 10), hora: r.fechaHora.substring(11, 16)
    });
  }

  guardar(): void {
    const f = this.form();
    if (!f) { return; }
    if (!f.clienteId || !f.empleadoId || !f.servicioId || !f.fecha || !f.hora) {
      this.errorForm.set('Completa cliente, servicio, terapeuta, fecha y hora.');
      return;
    }
    const body = {
      clienteId: f.clienteId, empleadoId: f.empleadoId, servicioId: f.servicioId, salaId: f.salaId,
      fechaHora: `${f.fecha}T${f.hora.length === 5 ? f.hora + ':00' : f.hora}`
    };
    this.enviando.set(true);
    const id = this.editandoId();
    (id ? this.admin.editarReserva(id, body) : this.admin.crearReserva(body)).subscribe({
      next: () => {
        this.enviando.set(false);
        this.form.set(null);
        this.aviso(id ? 'Reserva actualizada.' : 'Reserva creada en estado En espera.');
        this.cargar(true);
      },
      error: e => { this.enviando.set(false); this.errorForm.set(e.message); }
    });
  }
}
