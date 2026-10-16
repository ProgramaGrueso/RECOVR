import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../services/admin-api.service';
import { ApiService } from '../../../services/api.service';
import { SalaEstado, ServicioOps } from '../../../models/ops.model';
import { SALA_ESTADO_TXT, fechaHoraTxt, horaTxt, soles } from './ops.util';

/**
 * Catálogo de servicios y cabinas: CRUD sobre /api/servicios y /api/salas (solo ADMIN escribe).
 * Un servicio o cabina con reservas no se puede eliminar (se conserva el historial contable).
 */
@Component({
  selector: 'app-ops-catalogo',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (api.session()) {
      @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }

      <div class="ops-head"><h3 class="ops-title">Servicios y tarifas</h3><button type="button" class="ops-btn primary" (click)="abrirServicio(null)">+ Nuevo servicio</button></div>
      <div class="ops-table-wrap" style="margin-bottom:2rem">
        <table class="ops-table">
          <thead><tr><th>ID</th><th>Servicio</th><th>Duración</th><th>Limpieza</th><th>Precio</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (s of servicios(); track s.id) {
              <tr>
                <td class="mono">SRV-{{ s.id }}</td><td><strong>{{ s.nombre }}</strong></td><td>{{ s.duracionMinutos }} min</td>
                <td>{{ s.tiempoLimpiezaMinutos }} min</td><td>{{ soles(s.precio) }}</td>
                <td><div class="ops-actions">
                  <button type="button" class="ops-btn sm" (click)="abrirServicio(s)">Editar</button>
                  <button type="button" class="ops-btn sm danger" (click)="eliminarServicio(s)">Eliminar</button>
                </div></td>
              </tr>
            } @empty { <tr><td colspan="6" class="ops-empty">No hay servicios.</td></tr> }
          </tbody>
        </table>
      </div>

      <div class="ops-head"><h3 class="ops-title">Cabinas ({{ salas().length }})</h3><button type="button" class="ops-btn primary" (click)="abrirSala(null)">+ Nueva cabina</button></div>
      <div class="ops-table-wrap">
        <table class="ops-table">
          <thead><tr><th>ID</th><th>Cabina</th><th>Capacidad</th><th>Estado ahora</th><th>Próxima reserva</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (s of salas(); track s.id) {
              <tr>
                <td class="mono">CAB-{{ s.id }}</td><td><strong>{{ s.nombre }}</strong></td><td>{{ s.capacidad }} persona(s)</td>
                <td><span class="badge" [class]="'badge ' + s.estado">{{ salaTxt[s.estado] }}</span>
                  @if (s.terapeuta) { <div class="muted">{{ s.terapeuta }} · hasta {{ hora(s.hasta) }}</div> }</td>
                <td>{{ fechaHora(s.proximaReserva) }}</td>
                <td><div class="ops-actions">
                  <button type="button" class="ops-btn sm" (click)="abrirSala(s)">Editar</button>
                  @if (s.estado === 'MANTENIMIENTO') { <button type="button" class="ops-btn sm ok" (click)="cambiarActivaSala(s, true)">Reactivar</button> }
                  @else { <button type="button" class="ops-btn sm warn" (click)="cambiarActivaSala(s, false)">Mantenimiento</button> }
                  <button type="button" class="ops-btn sm danger" (click)="eliminarSala(s)">Eliminar</button>
                </div></td>
              </tr>
            } @empty { <tr><td colspan="6" class="ops-empty">No hay cabinas.</td></tr> }
          </tbody>
        </table>
      </div>

      @if (formServicio(); as f) {
        <div class="modal-back" (click)="formServicio.set(null)">
          <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="guardarServicio()">
            <h3>{{ servicioId() ? 'Editar servicio' : 'Nuevo servicio' }}</h3>
            @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
            <div class="form-grid">
              <div class="field full"><label for="sv-nombre">Nombre</label><input id="sv-nombre" class="ops-input" name="nombre" [(ngModel)]="f.nombre" required></div>
              <div class="field"><label for="sv-dur">Duración (min)</label><input id="sv-dur" class="ops-input" type="number" min="1" max="480" name="duracion" [(ngModel)]="f.duracionMinutos" required></div>
              <div class="field"><label for="sv-lim">Limpieza (min)</label><input id="sv-lim" class="ops-input" type="number" min="0" max="120" name="limpieza" [(ngModel)]="f.tiempoLimpiezaMinutos"></div>
              <div class="field full"><label for="sv-precio">Precio (S/)</label><input id="sv-precio" class="ops-input" type="number" min="1" step="0.01" name="precio" [(ngModel)]="f.precio" required></div>
            </div>
            <div class="modal-foot"><button type="button" class="ops-btn" (click)="formServicio.set(null)">Volver</button><button type="submit" class="ops-btn primary">Guardar</button></div>
          </form>
        </div>
      }
      @if (formSala(); as f) {
        <div class="modal-back" (click)="formSala.set(null)">
          <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="guardarSala()">
            <h3>{{ salaId() ? 'Editar cabina' : 'Nueva cabina' }}</h3>
            @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
            <div class="form-grid">
              <div class="field full"><label for="sa-nombre">Nombre</label><input id="sa-nombre" class="ops-input" name="nombre" [(ngModel)]="f.nombre" required></div>
              <div class="field full"><label for="sa-cap">Capacidad (personas)</label><input id="sa-cap" class="ops-input" type="number" min="1" name="capacidad" [(ngModel)]="f.capacidad" required></div>
            </div>
            <div class="modal-foot"><button type="button" class="ops-btn" (click)="formSala.set(null)">Volver</button><button type="submit" class="ops-btn primary">Guardar</button></div>
          </form>
        </div>
      }
    }
  `
})
export class OpsCatalogoComponent implements OnInit {
  private admin = inject(AdminApiService);
  api = inject(ApiService);

  servicios = signal<ServicioOps[]>([]);
  salas = signal<SalaEstado[]>([]);
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  errorForm = signal('');
  formServicio = signal<Omit<ServicioOps, 'id'> | null>(null);
  servicioId = signal<number | null>(null);
  formSala = signal<{ nombre: string; capacidad: number } | null>(null);
  salaId = signal<number | null>(null);

  readonly salaTxt = SALA_ESTADO_TXT; soles = soles; hora = horaTxt; fechaHora = fechaHoraTxt;

  ngOnInit(): void {
    if (this.api.session()) { this.cargar(); }
  }

  private aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(): void {
    forkJoin({ servicios: this.admin.servicios(), salas: this.admin.estadoSalas() }).subscribe({
      next: d => { this.servicios.set(d.servicios); this.salas.set(d.salas); },
      error: e => this.aviso(e.message, true)
    });
  }

  abrirServicio(s: ServicioOps | null): void {
    this.servicioId.set(s?.id ?? null);
    this.errorForm.set('');
    this.formServicio.set({
      nombre: s?.nombre ?? '', duracionMinutos: s?.duracionMinutos ?? 60,
      tiempoLimpiezaMinutos: s?.tiempoLimpiezaMinutos ?? 15, precio: s?.precio ?? 0
    });
  }

  guardarServicio(): void {
    const f = this.formServicio();
    if (!f) { return; }
    const id = this.servicioId();
    (id ? this.admin.editarServicio(id, f) : this.admin.crearServicio(f)).subscribe({
      next: () => { this.formServicio.set(null); this.aviso(id ? 'Servicio actualizado.' : 'Servicio creado.'); this.cargar(); },
      error: e => this.errorForm.set(e.message)
    });
  }

  eliminarServicio(s: ServicioOps): void {
    if (confirm(`¿Eliminar el servicio "${s.nombre}"? Si tiene reservas, el sistema no lo permitirá.`)) {
      this.admin.eliminarServicio(s.id).subscribe({
        next: () => { this.aviso('Servicio eliminado.'); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }

  abrirSala(s: SalaEstado | null): void {
    this.salaId.set(s?.id ?? null);
    this.errorForm.set('');
    this.formSala.set({ nombre: s?.nombre ?? '', capacidad: s?.capacidad ?? 1 });
  }

  guardarSala(): void {
    const f = this.formSala();
    if (!f) { return; }
    const id = this.salaId();
    (id ? this.admin.editarSala(id, f) : this.admin.crearSala(f)).subscribe({
      next: () => { this.formSala.set(null); this.aviso(id ? 'Cabina actualizada.' : 'Cabina creada.'); this.cargar(); },
      error: e => this.errorForm.set(e.message)
    });
  }

  cambiarActivaSala(s: SalaEstado, valor: boolean): void {
    const texto = valor ? `¿Reactivar "${s.nombre}"? Volverá a recibir reservas.`
      : `¿Poner "${s.nombre}" en mantenimiento? No recibirá reservas hasta reactivarla (no se puede si tiene reservas activas).`;
    if (confirm(texto)) {
      this.admin.cambiarActivaSala(s.id, valor).subscribe({
        next: () => { this.aviso(valor ? 'Cabina reactivada.' : 'Cabina en mantenimiento.'); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }

  eliminarSala(s: SalaEstado): void {
    if (confirm(`¿Eliminar la cabina "${s.nombre}"? Si tiene reservas, el sistema no lo permitirá.`)) {
      this.admin.eliminarSala(s.id).subscribe({
        next: () => { this.aviso('Cabina eliminada.'); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }
}
