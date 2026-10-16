import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { ApiService } from '../../../services/api.service';
import { AusenciaOps, EmpleadoEstado } from '../../../models/ops.model';
import { EMPLEADO_ESTADO_TXT, fechaHoraTxt, horaTxt } from './ops.util';
import { prepararFoto } from './ops.imagen';

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
          <thead><tr><th>Foto</th><th>Terapeuta</th><th>Especialidad</th><th>Teléfono</th><th>Ahora</th><th>Próxima reserva</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (e of empleados(); track e.id) {
              <tr>
                <td>
                  @if (e.tieneFoto) { <img [src]="admin.urlFoto(e.id, versionTxt())" [alt]="'Foto de ' + e.nombre" style="width:46px; height:58px; object-fit:cover; border-radius:8px; display:block"> }
                  @else { <div style="width:46px; height:58px; border-radius:8px; display:grid; place-items:center; background:#130e1a; color:var(--ops-muted); font-size:1.3rem">{{ e.nombre.charAt(0) }}</div> }
                </td>
                <td><strong>{{ e.nombre }}</strong><div class="muted mono">EMP-{{ e.id }}</div></td><td>{{ e.especialidad || '—' }}</td><td>{{ e.telefono || '—' }}</td>
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
                  <button type="button" class="ops-btn sm info" [disabled]="subiendoId() === e.id" (click)="selector.click()">{{ subiendoId() === e.id ? 'Subiendo…' : 'Foto' }}</button>
                  <input #selector type="file" accept="image/*" hidden (change)="subirFoto(e, $event)">
                  @if (e.tieneFoto) { <button type="button" class="ops-btn sm" (click)="quitarFoto(e)">Quitar foto</button> }
                  <button type="button" class="ops-btn sm" (click)="abrirAusencias(e)">Ausencias</button>
                  @if (e.activo) { <button type="button" class="ops-btn sm warn" (click)="cambiarActivo(e, false)">Dar de baja</button> }
                  @else { <button type="button" class="ops-btn sm ok" (click)="cambiarActivo(e, true)">Reactivar</button> }
                  <button type="button" class="ops-btn sm danger" (click)="eliminar(e)">Eliminar</button>
                </div></td>
              </tr>
            } @empty { <tr><td colspan="7" class="ops-empty">No hay terapeutas.</td></tr> }
          </tbody>
        </table>
      </div>

      @if (ausentes(); as a) {
        <div class="modal-back" (click)="ausentes.set(null)">
          <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="guardarAusencia()">
            <h3>Ausencias · {{ a.nombre }}</h3>
            <p class="ops-sub" style="margin-bottom:0.75rem">Vacaciones, licencias o capacitaciones: en esas fechas no recibe reservas.</p>
            @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
            <ul class="muted" style="list-style:none; padding:0; margin:0 0 1rem">
              @for (x of listaAusencias(); track x.id) {
                <li style="display:flex; justify-content:space-between; gap:0.5rem; padding:0.3rem 0">
                  <span>{{ fechaHora(x.desde) }} → {{ fechaHora(x.hasta) }} · {{ x.motivo || 'Sin motivo' }}</span>
                  <button type="button" class="ops-btn sm danger" (click)="quitarAusencia(x.id)">Quitar</button>
                </li>
              } @empty { <li>No tiene ausencias registradas.</li> }
            </ul>
            <div class="form-grid">
              <div class="field"><label for="au-desde">Desde</label><input id="au-desde" class="ops-input" type="datetime-local" name="desde" [(ngModel)]="nuevaAusencia.desde" required></div>
              <div class="field"><label for="au-hasta">Hasta</label><input id="au-hasta" class="ops-input" type="datetime-local" name="hasta" [(ngModel)]="nuevaAusencia.hasta" required></div>
              <div class="field full"><label for="au-motivo">Motivo</label><input id="au-motivo" class="ops-input" name="motivo" maxlength="150" [(ngModel)]="nuevaAusencia.motivo" placeholder="Vacaciones"></div>
            </div>
            <div class="modal-foot">
              <button type="button" class="ops-btn" (click)="ausentes.set(null)">Cerrar</button>
              <button type="submit" class="ops-btn primary">Registrar ausencia</button>
            </div>
          </form>
        </div>
      }

      @if (form(); as f) {
        <div class="modal-back" (click)="form.set(null)">
          <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="guardar()">
            <h3>{{ editandoId() ? 'Editar terapeuta' : 'Nueva terapeuta' }}</h3>
            @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
            <div class="form-grid">
              <div class="field full"><label for="em-nombre">Nombre</label><input id="em-nombre" class="ops-input" name="nombre" [(ngModel)]="f.nombre" required></div>
              <div class="field full"><label for="em-esp">Especialidad</label><input id="em-esp" class="ops-input" name="especialidad" [(ngModel)]="f.especialidad"></div>
              <div class="field full"><label for="em-tel">Teléfono</label><input id="em-tel" class="ops-input" name="telefono" maxlength="20" [(ngModel)]="f.telefono"></div>
              <div class="field full"><label for="em-bio">Presentación pública</label><textarea id="em-bio" class="ops-input" rows="3" name="bio" maxlength="500" [(ngModel)]="f.bio"></textarea>
                <span class="muted">Es lo que lee el público en el sitio. La foto la sube la propia terapeuta (módulo Mi perfil) o la administración desde esta tabla, con su consentimiento.</span></div>
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
  admin = inject(AdminApiService);
  api = inject(ApiService);

  empleados = signal<EmpleadoEstado[]>([]);
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  form = signal<{ nombre: string; especialidad: string; telefono: string; bio: string } | null>(null);
  subiendoId = signal<number | null>(null);
  version = signal(Date.now());
  versionTxt = () => String(this.version());
  editandoId = signal<number | null>(null);
  errorForm = signal('');
  ausentes = signal<EmpleadoEstado | null>(null);
  listaAusencias = signal<AusenciaOps[]>([]);
  nuevaAusencia = { desde: '', hasta: '', motivo: '' };

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
    this.form.set({ nombre: e?.nombre ?? '', especialidad: e?.especialidad ?? '', telefono: e?.telefono ?? '', bio: e?.bio ?? '' });
  }

  abrirAusencias(e: EmpleadoEstado): void {
    this.errorForm.set('');
    this.nuevaAusencia = { desde: '', hasta: '', motivo: '' };
    this.ausentes.set(e);
    this.cargarAusencias(e.id);
  }

  private cargarAusencias(id: number): void {
    this.admin.ausencias(id).subscribe({ next: l => this.listaAusencias.set(l), error: err => this.errorForm.set(err.message) });
  }

  guardarAusencia(): void {
    const e = this.ausentes();
    if (!e) { return; }
    this.errorForm.set('');
    const { desde, hasta, motivo } = this.nuevaAusencia;
    this.admin.crearAusencia(e.id, { desde: `${desde}:00`.slice(0, 19), hasta: `${hasta}:00`.slice(0, 19), motivo }).subscribe({
      next: () => { this.nuevaAusencia = { desde: '', hasta: '', motivo: '' }; this.cargarAusencias(e.id); },
      error: err => this.errorForm.set(err.message)
    });
  }

  quitarAusencia(id: number): void {
    const e = this.ausentes();
    this.admin.eliminarAusencia(id).subscribe({
      next: () => { if (e) { this.cargarAusencias(e.id); } },
      error: err => this.errorForm.set(err.message)
    });
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

  async subirFoto(e: EmpleadoEstado, evento: Event): Promise<void> {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) { return; }
    this.subiendoId.set(e.id);
    try {
      const blob = await prepararFoto(archivo);
      this.admin.subirFotoEmpleado(e.id, blob).subscribe({
        next: () => { this.subiendoId.set(null); this.version.set(Date.now()); this.aviso(`Foto de ${e.nombre} actualizada.`); this.cargar(); },
        error: err => { this.subiendoId.set(null); this.aviso(err.message, true); }
      });
    } catch (err) {
      this.subiendoId.set(null);
      this.aviso((err as Error).message, true);
    }
  }

  quitarFoto(e: EmpleadoEstado): void {
    if (confirm(`¿Quitar la foto de ${e.nombre} del sitio?`)) {
      this.admin.quitarFotoEmpleado(e.id).subscribe({
        next: () => { this.version.set(Date.now()); this.aviso('Foto eliminada.'); this.cargar(); },
        error: err => this.aviso(err.message, true)
      });
    }
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
