import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { ApiService } from '../../../services/api.service';
import { ClienteOps } from '../../../models/ops.model';
import { OpsGateComponent } from './ops-gate.component';

/** Directorio de clientes: CRUD sobre /api/clientes. Un cliente con reservas no se puede eliminar. */
@Component({
  selector: 'app-ops-clientes',
  standalone: true,
  imports: [FormsModule, OpsGateComponent],
  styleUrl: './ops.scss',
  template: `
    <app-ops-gate (connected)="cargar()" />
    @if (api.session()) {
      @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }
      <div class="ops-head">
        <input class="ops-input" type="search" placeholder="Buscar por nombre o correo" aria-label="Buscar cliente" [ngModel]="texto()" (ngModelChange)="texto.set($event)">
        <button type="button" class="ops-btn primary" (click)="abrir(null)">+ Nuevo cliente</button>
      </div>
      <div class="ops-table-wrap">
        <table class="ops-table">
          <thead><tr><th>ID</th><th>Nombre</th><th>Correo</th><th>Teléfono</th><th>Cuenta web</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (c of visibles(); track c.id) {
              <tr>
                <td class="mono">CLI-{{ c.id }}</td><td><strong>{{ c.nombre }}</strong></td><td>{{ c.correo }}</td><td>{{ c.telefono || '—' }}</td>
                <td>@if (c.usuarioId) { <span class="badge COMPLETADA">Con cuenta</span> } @else { <span class="muted">Sin cuenta (walk-in)</span> }</td>
                <td><div class="ops-actions">
                  <button type="button" class="ops-btn sm" (click)="abrir(c)">Editar</button>
                  <button type="button" class="ops-btn sm danger" (click)="eliminar(c)">Eliminar</button>
                </div></td>
              </tr>
            } @empty { <tr><td colspan="6" class="ops-empty">No hay clientes.</td></tr> }
          </tbody>
        </table>
      </div>
      <p class="ops-sub" style="margin-top:0.6rem">{{ visibles().length }} de {{ clientes().length }} clientes</p>

      @if (form(); as f) {
        <div class="modal-back" (click)="form.set(null)">
          <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="guardar()">
            <h3>{{ editandoId() ? 'Editar cliente' : 'Nuevo cliente' }}</h3>
            @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
            <div class="form-grid">
              <div class="field full"><label for="cl-nombre">Nombre completo</label><input id="cl-nombre" class="ops-input" name="nombre" [(ngModel)]="f.nombre" required></div>
              <div class="field"><label for="cl-correo">Correo</label><input id="cl-correo" class="ops-input" type="email" name="correo" [(ngModel)]="f.correo" required></div>
              <div class="field"><label for="cl-tel">Teléfono</label><input id="cl-tel" class="ops-input" name="telefono" maxlength="20" [(ngModel)]="f.telefono"></div>
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
export class OpsClientesComponent implements OnInit {
  private admin = inject(AdminApiService);
  api = inject(ApiService);

  clientes = signal<ClienteOps[]>([]);
  texto = signal('');
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  form = signal<{ nombre: string; correo: string; telefono: string } | null>(null);
  editandoId = signal<number | null>(null);
  errorForm = signal('');

  visibles = computed(() => {
    const q = this.texto().trim().toLowerCase();
    return this.clientes().filter(c => !q || c.nombre.toLowerCase().includes(q) || c.correo.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    if (this.api.session()) { this.cargar(); }
  }

  private aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(): void {
    this.admin.clientes().subscribe({ next: c => this.clientes.set(c), error: e => this.aviso(e.message, true) });
  }

  abrir(c: ClienteOps | null): void {
    this.editandoId.set(c?.id ?? null);
    this.errorForm.set('');
    this.form.set({ nombre: c?.nombre ?? '', correo: c?.correo ?? '', telefono: c?.telefono ?? '' });
  }

  guardar(): void {
    const f = this.form();
    if (!f) { return; }
    const id = this.editandoId();
    (id ? this.admin.editarCliente(id, f) : this.admin.crearCliente(f)).subscribe({
      next: () => { this.form.set(null); this.aviso(id ? 'Cliente actualizado.' : 'Cliente creado.'); this.cargar(); },
      error: e => this.errorForm.set(e.message)
    });
  }

  eliminar(c: ClienteOps): void {
    if (confirm(`¿Eliminar al cliente ${c.nombre}? Si tiene reservas, el sistema no lo permitirá.`)) {
      this.admin.eliminarCliente(c.id).subscribe({
        next: () => { this.aviso('Cliente eliminado.'); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }
}
