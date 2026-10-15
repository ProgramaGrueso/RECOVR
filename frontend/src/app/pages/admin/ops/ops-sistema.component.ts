import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../services/admin-api.service';
import { AuditoriaOps, EstadoSistemaOps } from '../../../models/ops.model';
import { fechaHoraTxt } from './ops.util';

/** Estado real del sistema y auditoría: quién hizo qué, desde dónde y con qué resultado. Solo ADMIN. */
@Component({
  selector: 'app-ops-sistema',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (error()) { <div class="ops-banner err">{{ error() }}</div> }
    @if (estado(); as s) {
      <div class="kpi-grid">
        <div class="kpi"><div class="k-label">Base de datos</div>
          <div class="k-value" [style.color]="s.baseDatosActiva ? 'var(--ops-ok)' : 'var(--ops-bad)'">{{ s.baseDatosActiva ? 'Activa' : 'Caída' }}</div><div class="k-note">{{ s.baseDatos }}</div></div>
        <div class="kpi"><div class="k-label">Tiempo activo</div><div class="k-value">{{ uptime(s.segundosActivo) }}</div><div class="k-note">Desde {{ fechaHora(s.iniciadoEn) }}</div></div>
        <div class="kpi"><div class="k-label">Memoria</div><div class="k-value">{{ s.memoriaUsadaMb }} MB</div><div class="k-note">de {{ s.memoriaMaxMb }} MB · Java {{ s.java }} · Spring Boot {{ s.springBoot }}</div></div>
        <div class="kpi"><div class="k-label">Accesos denegados (24 h)</div><div class="k-value" [style.color]="s.intentosDenegados24h > 0 ? 'var(--ops-warn)' : ''">{{ s.intentosDenegados24h }}</div><div class="k-note">Logins fallidos, 401, 403 y bloqueos</div></div>
      </div>

      <div class="state-grid">
        <div class="ops-card"><h4 class="ops-title">Registros en la base</h4>
          @for (k of claves(s.registros); track k) { <div class="ops-row" style="justify-content:space-between"><span>{{ k }}</span><strong>{{ s.registros[k] }}</strong></div> }</div>
        <div class="ops-card"><h4 class="ops-title">Reservas por estado</h4>
          @for (k of claves(s.reservasPorEstado); track k) { <div class="ops-row" style="justify-content:space-between"><span>{{ k }}</span><strong>{{ s.reservasPorEstado[k] }}</strong></div> }</div>
        <div class="ops-card"><h4 class="ops-title">Usuarios por rol</h4>
          @for (k of claves(s.usuariosPorRol); track k) { <div class="ops-row" style="justify-content:space-between"><span>{{ k }}</span><strong>{{ s.usuariosPorRol[k] }}</strong></div> }</div>
        <div class="ops-card"><h4 class="ops-title">Ciclo de vida de reservas</h4>
          <p class="ops-sub">Se ejecuta al arrancar y cada minuto.</p>
          <div class="ops-row" style="justify-content:space-between"><span>Última ejecución</span><strong>{{ s.cicloUltimaEjecucion ? fechaHora(s.cicloUltimaEjecucion) : 'aún no' }}</strong></div>
          <div class="ops-row" style="justify-content:space-between"><span>Cambios en la última</span><strong>{{ s.cicloUltimosCambios }}</strong></div>
          <div class="ops-row" style="justify-content:space-between"><span>Cambios desde el arranque</span><strong>{{ s.cicloTotalCambios }}</strong></div></div>
      </div>
    }

    <div class="ops-head"><h3 class="ops-title">Auditoría (últimos eventos)</h3>
      <div class="ops-row">
        <select class="ops-select" aria-label="Filtrar auditoría" [ngModel]="filtro()" (ngModelChange)="filtro.set($event)">
          <option value="">Todo</option><option value="DENEGADOS">Solo denegados y fallidos</option><option value="CAMBIOS">Solo cambios de datos</option><option value="LOGINS">Solo inicios de sesión</option>
        </select>
        <button type="button" class="ops-btn" (click)="cargar()">↻ Actualizar</button></div></div>
    <div class="ops-table-wrap">
      <table class="ops-table">
        <thead><tr><th>Fecha</th><th>Usuario</th><th>Rol</th><th>Acción</th><th>Recurso</th><th>Resultado</th><th>IP</th></tr></thead>
        <tbody>
          @for (a of visibles(); track a.id) {
            <tr>
              <td>{{ fechaHora(a.fecha) }}</td><td>{{ a.usuario }}</td><td>{{ a.rol }}</td>
              <td><strong>{{ a.accion }}</strong></td><td class="mono">{{ a.recurso }}</td>
              <td><span class="badge" [class]="'badge ' + (malo(a) ? 'CANCELADA' : 'COMPLETADA')">{{ a.estado }}</span>@if (a.detalle) { <div class="muted">{{ a.detalle }}</div> }</td>
              <td class="mono">{{ a.ip }}</td>
            </tr>
          } @empty { <tr><td colspan="7" class="ops-empty">Sin eventos para este filtro.</td></tr> }
        </tbody>
      </table>
    </div>
  `
})
export class OpsSistemaComponent implements OnInit, OnDestroy {
  private admin = inject(AdminApiService);

  estado = signal<EstadoSistemaOps | null>(null);
  eventos = signal<AuditoriaOps[]>([]);
  filtro = signal('');
  error = signal('');
  fechaHora = fechaHoraTxt;

  visibles = computed(() => this.eventos().filter(a => {
    switch (this.filtro()) {
      case 'DENEGADOS': return this.malo(a);
      case 'CAMBIOS': return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(a.accion);
      case 'LOGINS': return a.accion.startsWith('LOGIN');
      default: return true;
    }
  }));

  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.cargar();
    this.timer = setInterval(() => this.cargar(), 30_000);
  }

  ngOnDestroy(): void {
    if (this.timer) { clearInterval(this.timer); }
  }

  claves(m: Record<string, number>): string[] {
    return Object.keys(m);
  }

  malo(a: AuditoriaOps): boolean {
    return a.accion === 'LOGIN_FALLIDO' || a.accion === 'LOGIN_BLOQUEADO' || (a.estado ?? 200) >= 400;
  }

  uptime(seg: number): string {
    const h = Math.floor(seg / 3600), m = Math.floor((seg % 3600) / 60);
    return h > 0 ? `${h} h ${m} min` : `${m} min`;
  }

  cargar(): void {
    forkJoin({ estado: this.admin.estadoSistema(), eventos: this.admin.auditoria(150) }).subscribe({
      next: d => { this.estado.set(d.estado); this.eventos.set(d.eventos); this.error.set(''); },
      error: e => this.error.set(e.message)
    });
  }
}
