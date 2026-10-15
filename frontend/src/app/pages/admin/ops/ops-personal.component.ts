import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { ApiService } from '../../../services/api.service';
import { EmpleadoEstado } from '../../../models/ops.model';
import { EMPLEADO_ESTADO_TXT, fechaHoraTxt, horaTxt } from './ops.util';

/**
 * Personal (masajistas): CRUD sobre /api/empleados (solo ADMIN escribe). El estado (Libre / En sesión / Por iniciar)
 * se calcula en vivo con las reservas. Con historial de reservas no se elimina: se da de baja (deja de recibir reservas
 * pero conserva su historial); la baja exige que no tenga reservas activas.
 */
@Component({
  selector: 'app-ops-personal',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (api.session()) {
      @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }
      <div class="ops-head">
        <p class="ops-sub" style="margin:0">Quién atiende, su especialidad y su disponibilidad en este momento.</p>
        <button type="button" class="ops-btn primary" (click)="abrir(null)">+ Nueva terapeuta</button>
      </div>
      <div class="ops-table-wrap">
        <table class="ops-table">
          <thead><tr><th>ID</th><th>Terapeuta</th><th>Especialidad</th><th>Teléfono</th><th>Ahora</th><th>Próxima reserva</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (e of empleados(); track e.id) {
              <tr>
                <td class="mono">EMP-{{ e.id }}</td><td><strong>{{ e.nombre }}</strong></td><td>{{ e.especialidad || '—' }}</td><td>{{ e.telefono || '—' }}</td>
                <td>
                  @if (!e.activo) { <span class="badge CANCELADA">De baja</span> }
                  @else {
                    <span class="badge" [class]="'badge ' + e.estado">{{ estTxt[e.estado] }}</span>
                    @if (e.estado !== 'LIBRE') { <div class="muted">{{ e.sala }} · hasta {{ hora(e.hasta) }}</div> }
                  }
                </td>
                <td>{{ fechaHora(e.proximaReserva) }}</td>
                <td><div class="ops-actions">
                  <button type="button" class="ops-btn sm" (click)="abrir(e)">Editar</button>
                  @if (e.activo) { <button type="button" class="ops-btn sm warn" (click)="cambiarActivo(e, false)">Dar de baja</button> }
                  @else { <button type="button" class="ops-btn sm ok" (click)="cambiarActivo(e, true)">Reactivar</button> }
                  <button type="button" class="ops-btn sm danger" (click)="eliminar(e)">Eliminar</button>
                </div></td>
              </tr>
            } @empty { <tr><td colspan="7" class="ops-empty">No hay terapeutas.</td></tr> }
          </tbody>
        </table>
      </div>

      @if (form(); as f) {
        <div class="modal-back" (click)="form.set(null)">
          <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="guardar()">
            <h3>{{ editandoId() ? 'Editar terapeuta' : 'Nueva terapeuta' }}</h3>
            @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
            <div class="form-grid">
              <div class="field full"><label for="em-nombre">Nombre</label><input id="em-nombre" class="ops-input" name="nombre" [(ngModel)]="f.nombre" required></div>
              <div class="field full"><label for="em-esp">Especialidad</label><input id="em-esp" class="ops-input" name="especialidad" [(ngModel)]="f.especialidad"></div>
              <div class="field full"><label for="em-tel">Teléfono</label><input id="em-tel" class="ops-input" name="telefono" maxlength="20" [(ngModel)]="f.telefono"></div>
            </div>
            <div class="modal-foot">
              <button type="button" class="ops-btn" (click)="form.set(null)">Volver</button>
              <button type="submit" class="ops-btn primary">Guardar</button>
            </div>
          </form>
        </div>
      }
    }
  `
})
export class OpsPersonalComponent implements OnInit {
  private admin = inject(AdminApiService);
  api = inject(ApiService);

  empleados = signal<EmpleadoEstado[]>([]);
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  form = signal<{ nombre: string; especialidad: string; telefono: string } | null>(null);
  editandoId = signal<number | null>(null);
  errorForm = signal('');

  readonly estTxt = EMPLEADO_ESTADO_TXT; hora = horaTxt; fechaHora = fechaHoraTxt;

  ngOnInit(): void {
    if (this.api.session()) { this.cargar(); }
  }

  private aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(): void {
    this.admin.estadoEmpleados().subscribe({ next: e => this.empleados.set(e), error: e => this.aviso(e.message, true) });
  }

  abrir(e: EmpleadoEstado | null): void {
    this.editandoId.set(e?.id ?? null);
    this.errorForm.set('');
    this.form.set({ nombre: e?.nombre ?? '', especialidad: e?.especialidad ?? '', telefono: e?.telefono ?? '' });
  }

  guardar(): void {
    const f = this.form();
    if (!f) { return; }
    const id = this.editandoId();
    (id ? this.admin.editarEmpleado(id, f) : this.admin.crearEmpleado(f)).subscribe({
      next: () => { this.form.set(null); this.aviso(id ? 'Terapeuta actualizada.' : 'Terapeuta creada.'); this.cargar(); },
      error: e => this.errorForm.set(e.message)
    });
  }

  cambiarActivo(e: EmpleadoEstado, valor: boolean): void {
    const texto = valor ? `¿Reactivar a ${e.nombre}? Volverá a recibir reservas.`
      : `¿Dar de baja a ${e.nombre}? Conserva su historial pero deja de recibir reservas.`;
    if (confirm(texto)) {
      this.admin.cambiarActivoEmpleado(e.id, valor).subscribe({
        next: () => { this.aviso(valor ? `${e.nombre} fue reactivada.` : `${e.nombre} fue dada de baja.`); this.cargar(); },
        error: err => this.aviso(err.message, true)
      });
    }
  }

  eliminar(e: EmpleadoEstado): void {
    if (confirm(`¿Eliminar a ${e.nombre} del personal? Si tiene reservas registradas, el sistema no lo permitirá.`)) {
      this.admin.eliminarEmpleado(e.id).subscribe({
        next: () => { this.aviso(`${e.nombre} fue eliminada del personal.`); this.cargar(); },
        error: err => this.aviso(err.message, true)
      });
    }
  }
}
