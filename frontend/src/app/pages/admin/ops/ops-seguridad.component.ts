import { Component, OnInit, inject, signal } from '@angular/core';
import { AdminApiService } from '../../../services/admin-api.service';
import { PermisoOps } from '../../../models/ops.model';

/**
 * Matriz de permisos por rol, leída del backend. Una prueba automática ejecuta cada fila con un token de cada rol,
 * así que lo que se muestra aquí coincide con lo que la API realmente permite.
 */
@Component({
  selector: 'app-ops-seguridad',
  standalone: true,
  styleUrl: './ops.scss',
  template: `
    @if (error()) { <div class="ops-banner err">{{ error() }}</div> }
    <div class="ops-banner info">Esta tabla la entrega el servidor y está verificada por pruebas automáticas: cada fila se ejecuta con un token de cada rol. ✓ = permitido · — = el servidor responde 403.</div>
    <div class="ops-table-wrap">
      <table class="ops-table" style="min-width:900px">
        <thead><tr><th>Acción</th><th>Endpoint</th>@for (r of roles; track r) { <th style="text-align:center">{{ r }}</th> }<th>Nota</th></tr></thead>
        <tbody>
          @for (p of permisos(); track p.metodo + p.ruta) {
            <tr>
              <td><strong>{{ p.accion }}</strong></td><td class="mono">{{ p.metodo }} {{ p.ruta }}</td>
              @for (r of roles; track r) {
                <td style="text-align:center"><span [style.color]="p.roles.includes(r) ? 'var(--ops-ok)' : 'var(--ops-muted)'" style="font-weight:800">{{ p.roles.includes(r) ? '✓' : '—' }}</span></td>
              }
              <td class="muted">{{ p.nota }}</td>
            </tr>
          } @empty { <tr><td [attr.colspan]="roles.length + 3" class="ops-empty">Cargando…</td></tr> }
        </tbody>
      </table>
    </div>
  `
})
export class OpsSeguridadComponent implements OnInit {
  private admin = inject(AdminApiService);
  permisos = signal<PermisoOps[]>([]);
  error = signal('');
  readonly roles = ['ADMIN', 'RECEPCIONISTA', 'ESPECIALISTA', 'CLIENTE'];

  ngOnInit(): void {
    this.admin.matriz().subscribe({ next: p => this.permisos.set(p), error: e => this.error.set(e.message) });
  }
}
