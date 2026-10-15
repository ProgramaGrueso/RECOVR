import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../services/admin-api.service';
import { AuthService } from '../../../services/auth.service';
import { EmpleadoEstado, UsuarioNuevoOps, UsuarioOps } from '../../../models/ops.model';

/**
 * Usuarios y accesos (ADMIN). Aquí se responde "¿quién crea a quién?":
 * los clientes se registran solos; recepción, administración y terapeutas los crea un administrador, con una clave
 * temporal que se muestra una sola vez y que la persona debe cambiar en su primer ingreso.
 */
@Component({
  selector: 'app-ops-usuarios',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }
    <div class="ops-banner info">Los clientes se registran solos en el sitio. Recepción, administración y terapeutas los crea un administrador desde aquí: el sistema genera una clave temporal (se muestra una sola vez) y la persona debe cambiarla al ingresar.</div>

    <div class="ops-head">
      <div class="ops-row">
        @for (f of filtros; track f.key) { <button type="button" class="ops-chip" [class.on]="filtro() === f.key" (click)="filtro.set(f.key)">{{ f.label }}</button> }
      </div>
      <div class="ops-row">
        <button type="button" class="ops-btn" (click)="cargar()">↻ Actualizar</button>
        <button type="button" class="ops-btn primary" (click)="abrir()">+ Nuevo acceso</button>
      </div>
    </div>

    <div class="ops-table-wrap">
      <table class="ops-table">
        <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Vínculo</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>
          @for (u of visibles(); track u.id) {
            <tr>
              <td><strong>{{ u.nombre }}</strong></td><td>{{ u.correo }}</td><td>{{ u.rol }}</td><td>{{ u.vinculo }}</td>
              <td>
                @if (!u.activo) { <span class="badge CANCELADA">Desactivado</span> }
                @else if (u.debeCambiarClave) { <span class="badge EN_ESPERA">Clave temporal pendiente</span> }
                @else { <span class="badge COMPLETADA">Activo</span> }
              </td>
              <td><div class="ops-actions">
                @if (u.activo) { <button type="button" class="ops-btn sm warn" (click)="cambiarActivo(u, false)">Desactivar</button> }
                @else { <button type="button" class="ops-btn sm ok" (click)="cambiarActivo(u, true)">Activar</button> }
                <button type="button" class="ops-btn sm" (click)="restablecer(u)">Restablecer clave</button>
              </div></td>
            </tr>
          } @empty { <tr><td colspan="6" class="ops-empty">No hay usuarios para este filtro.</td></tr> }
        </tbody>
      </table>
    </div>
    <p class="ops-sub" style="margin-top:0.6rem">Un administrador no puede desactivarse a sí mismo ni desactivar al último administrador activo. Una cuenta desactivada pierde el acceso al instante.</p>

    @if (form(); as f) {
      <div class="modal-back" (click)="form.set(null)">
        <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="crear()">
          <h3>Nuevo acceso del personal</h3>
          @if (errorForm()) { <div class="ops-banner err">{{ errorForm() }}</div> }
          <div class="form-grid">
            <div class="field full"><label for="us-nombre">Nombre completo</label><input id="us-nombre" class="ops-input" name="nombre" [(ngModel)]="f.nombre" required minlength="3"></div>
            <div class="field full"><label for="us-correo">Correo (será su usuario)</label><input id="us-correo" class="ops-input" type="email" name="correo" [(ngModel)]="f.correo" required></div>
            <div class="field"><label for="us-rol">Rol</label>
              <select id="us-rol" class="ops-select" name="rol" [(ngModel)]="f.rol">
                <option value="RECEPCIONISTA">Recepción</option><option value="ESPECIALISTA">Terapeuta</option><option value="ADMIN">Administración</option>
              </select></div>
            @if (f.rol === 'ESPECIALISTA') {
              <div class="field"><label for="us-ter">Terapeuta</label>
                <select id="us-ter" class="ops-select" name="empleadoId" [(ngModel)]="f.empleadoId" required>
                  <option [ngValue]="null" disabled>Selecciona</option>
                  @for (t of terapeutasSinAcceso(); track t.id) { <option [ngValue]="t.id">{{ t.nombre }}</option> }
                </select></div>
            }
          </div>
          @if (f.rol === 'ESPECIALISTA' && terapeutasSinAcceso().length === 0) { <p class="ops-sub" style="margin-top:0.6rem">Todas las terapeutas ya tienen acceso. Crea primero a la terapeuta en el módulo Personal.</p> }
          <div class="modal-foot"><button type="button" class="ops-btn" (click)="form.set(null)">Volver</button><button type="submit" class="ops-btn primary">Crear acceso</button></div>
        </form>
      </div>
    }

    @if (resultado(); as r) {
      <div class="modal-back">
        <div class="modal" style="text-align:center">
          <h3>Clave temporal de {{ r.usuario.nombre }}</h3>
          <p class="ops-sub">Cópiala y entrégasela en persona. <strong>No volverá a mostrarse.</strong> Al ingresar deberá definir una contraseña propia.</p>
          <div style="font-family:ui-monospace,monospace; font-size:1.6rem; letter-spacing:0.12em; margin:1.2rem 0; padding:0.9rem; border:1px dashed var(--ops-pink); border-radius:10px; user-select:all">{{ r.claveTemporal }}</div>
          <p class="ops-sub">Usuario: <strong>{{ r.usuario.correo }}</strong></p>
          <div class="modal-foot" style="justify-content:center">
            <button type="button" class="ops-btn" (click)="copiar(r.claveTemporal)">{{ copiada() ? '✓ Copiada' : 'Copiar clave' }}</button>
            <button type="button" class="ops-btn primary" (click)="resultado.set(null); copiada.set(false)">Ya la anoté</button>
          </div>
        </div>
      </div>
    }
  `
})
export class OpsUsuariosComponent implements OnInit {
  private admin = inject(AdminApiService);
  private auth = inject(AuthService);

  usuarios = signal<UsuarioOps[]>([]);
  empleados = signal<EmpleadoEstado[]>([]);
  filtro = signal<'PERSONAL' | 'CLIENTE' | 'TODOS'>('PERSONAL');
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  form = signal<{ nombre: string; correo: string; rol: string; empleadoId: number | null } | null>(null);
  errorForm = signal('');
  resultado = signal<UsuarioNuevoOps | null>(null);
  copiada = signal(false);

  readonly filtros = [
    { key: 'PERSONAL' as const, label: 'Personal' }, { key: 'CLIENTE' as const, label: 'Clientes' }, { key: 'TODOS' as const, label: 'Todos' }
  ];

  visibles = computed(() => this.usuarios().filter(u =>
    this.filtro() === 'TODOS' || (this.filtro() === 'CLIENTE' ? u.rol === 'CLIENTE' : u.rol !== 'CLIENTE')));

  terapeutasSinAcceso = computed(() => {
    const conAcceso = new Set(this.usuarios().filter(u => u.empleadoId !== null).map(u => u.empleadoId));
    return this.empleados().filter(e => e.activo && !conAcceso.has(e.id));
  });

  ngOnInit(): void {
    this.cargar();
  }

  private aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(): void {
    forkJoin({ usuarios: this.admin.usuarios(), empleados: this.admin.estadoEmpleados() }).subscribe({
      next: d => { this.usuarios.set(d.usuarios); this.empleados.set(d.empleados); },
      error: e => this.aviso(e.message, true)
    });
  }

  abrir(): void {
    this.errorForm.set('');
    this.form.set({ nombre: '', correo: '', rol: 'RECEPCIONISTA', empleadoId: null });
  }

  crear(): void {
    const f = this.form();
    if (!f) { return; }
    this.admin.crearUsuario({ ...f, empleadoId: f.rol === 'ESPECIALISTA' ? f.empleadoId : null }).subscribe({
      next: r => { this.form.set(null); this.resultado.set(r); this.cargar(); },
      error: e => this.errorForm.set(e.message)
    });
  }

  cambiarActivo(u: UsuarioOps, valor: boolean): void {
    const texto = valor ? `¿Reactivar el acceso de ${u.nombre}?` : `¿Desactivar el acceso de ${u.nombre}? Perderá el acceso al instante (conserva su historial).`;
    if (confirm(texto)) {
      this.admin.activarUsuario(u.id, valor).subscribe({
        next: () => { this.aviso(valor ? 'Acceso reactivado.' : 'Acceso desactivado.'); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }

  restablecer(u: UsuarioOps): void {
    if (u.correo === this.auth.user()?.correo) {
      this.aviso('Para cambiar tu propia contraseña usa "Cambiar contraseña" del menú lateral.', true);
      return;
    }
    if (confirm(`¿Restablecer la clave de ${u.nombre}? Su clave actual dejará de funcionar y deberá usar una temporal.`)) {
      this.admin.restablecerClave(u.id).subscribe({
        next: r => { this.resultado.set(r); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }

  copiar(clave: string): void {
    void navigator.clipboard?.writeText(clave).then(() => this.copiada.set(true));
  }
}
