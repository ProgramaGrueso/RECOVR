import { Component, EventEmitter, OnDestroy, OnInit, Output, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../services/admin-api.service';
import { ApiService } from '../../../services/api.service';
import { EmpleadoEstado, ReservaOps, ResumenOps, SalaEstado } from '../../../models/ops.model';
import { EMPLEADO_ESTADO_TXT, ESTADO_TXT, SALA_ESTADO_TXT, codigoReserva, fechaHoraTxt, fechaInput, horaTxt, soles } from './ops.util';

/** Panel de control: indicadores y estado en vivo, todo calculado por el backend desde las reservas. */
@Component({
  selector: 'app-ops-resumen',
  standalone: true,
  imports: [],
  styleUrl: './ops.scss',
  template: `
    @if (api.session()) {
      @if (error()) { <div class="ops-banner err">{{ error() }}</div> }
      @if (resumen(); as r) {
        <div class="kpi-grid">
          <div class="kpi"><div class="k-label">Ingresos del mes</div><div class="k-value">{{ soles(r.ingresosMes) }}</div><div class="k-note">Pagos cobrados, sin reservas canceladas</div></div>
          <div class="kpi"><div class="k-label">Ocupación de hoy</div><div class="k-value">{{ r.ocupacionHoyPct }}%</div><div class="k-note">Minutos reservados ÷ (cabinas × 11 h de atención)</div></div>
          <div class="kpi"><div class="k-label">Sesiones completadas</div><div class="k-value">{{ r.sesionesCompletadasMes }}</div><div class="k-note">{{ r.agendadasHoy }} activas para hoy · {{ r.pendientesPorConfirmar }} por confirmar</div></div>
          <div class="kpi"><div class="k-label">Tasa de cancelación</div><div class="k-value">{{ r.tasaCancelacionPct }}%</div><div class="k-note">{{ r.canceladasMes }} de {{ r.totalMes }} reservas del mes</div></div>
        </div>
      }

      <div class="ops-head"><h3 class="ops-title">Disponibilidad de terapeutas</h3><span class="muted">● actualización en vivo</span></div>
      <div class="state-grid">
        @for (e of empleados(); track e.id) {
          <div class="state-card" [class]="'state-card ' + e.estado">
            <div class="s-top"><h4>{{ e.nombre }}</h4><span class="badge" [class]="'badge ' + e.estado">{{ empTxt[e.estado] }}</span></div>
            <p>{{ e.especialidad }}</p>
            @if (e.estado !== 'LIBRE') { <p>{{ e.sala }} · hasta {{ hora(e.hasta) }}</p> }
            @if (e.proximaReserva) { <p>Próxima: {{ fechaHora(e.proximaReserva) }}</p> } @else { <p>Sin reservas próximas</p> }
          </div>
        } @empty { <div class="ops-empty">Aún no hay terapeutas registradas.</div> }
      </div>

      <div class="ops-head"><h3 class="ops-title">Estado en tiempo real de las cabinas</h3><span class="muted">{{ salas().length }} cabinas</span></div>
      <div class="state-grid">
        @for (s of salas(); track s.id) {
          <div class="state-card" [class]="'state-card ' + s.estado">
            <div class="s-top"><h4>{{ s.nombre }}</h4><span class="badge" [class]="'badge ' + s.estado">{{ salaTxt[s.estado] }}</span></div>
            <p>Capacidad: {{ s.capacidad }} persona(s)</p>
            @if (s.terapeuta) { <p>Terapeuta: {{ s.terapeuta }} · hasta {{ hora(s.hasta) }}</p> }
            @if (s.proximaReserva) { <p>Próxima reserva: {{ fechaHora(s.proximaReserva) }}</p> } @else { <p>Sin reservas próximas</p> }
          </div>
        } @empty { <div class="ops-empty">Aún no hay cabinas registradas.</div> }
      </div>

      <div class="ops-head">
        <h3 class="ops-title">Flujo de atención inmediato (hoy)</h3>
        <button type="button" class="ops-btn sm" (click)="irAgenda.emit()">Ver agenda completa →</button>
      </div>
      <div class="ops-table-wrap">
        <table class="ops-table">
          <thead><tr><th>Código</th><th>Cliente</th><th>Servicio</th><th>Terapeuta</th><th>Cabina</th><th>Horario</th><th>Estado</th></tr></thead>
          <tbody>
            @for (r of deHoy(); track r.id) {
              <tr>
                <td class="mono">{{ codigo(r.id) }}</td><td><strong>{{ r.clienteNombre }}</strong></td><td>{{ r.servicioNombre }}</td>
                <td>{{ r.empleadoNombre }}</td><td>{{ r.salaNombre }}</td><td>{{ hora(r.fechaHora) }}</td>
                <td><span class="badge" [class]="'badge ' + r.estadoOperativo">{{ estadoTxt[r.estadoOperativo] }}</span></td>
              </tr>
            } @empty { <tr><td colspan="7" class="ops-empty">No hay reservas para hoy.</td></tr> }
          </tbody>
        </table>
      </div>
    }
  `
})
export class OpsResumenComponent implements OnInit, OnDestroy {
  private admin = inject(AdminApiService);
  api = inject(ApiService);
  @Output() irAgenda = new EventEmitter<void>();

  resumen = signal<ResumenOps | null>(null);
  empleados = signal<EmpleadoEstado[]>([]);
  salas = signal<SalaEstado[]>([]);
  reservas = signal<ReservaOps[]>([]);
  error = signal('');

  readonly estadoTxt = ESTADO_TXT; readonly empTxt = EMPLEADO_ESTADO_TXT; readonly salaTxt = SALA_ESTADO_TXT;
  codigo = codigoReserva; hora = horaTxt; fechaHora = fechaHoraTxt; soles = soles;

  deHoy = computed(() => {
    const hoy = fechaInput();
    return this.reservas().filter(r => r.fechaHora.startsWith(hoy) && r.estadoOperativo !== 'CANCELADA')
      .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora));
  });

  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    if (this.api.session()) { this.cargar(); }
    this.timer = setInterval(() => { if (this.api.session()) { this.cargar(); } }, 30_000);
  }

  ngOnDestroy(): void {
    if (this.timer) { clearInterval(this.timer); }
  }

  cargar(): void {
    forkJoin({
      resumen: this.admin.resumen(), empleados: this.admin.estadoEmpleados(),
      salas: this.admin.estadoSalas(), reservas: this.admin.reservas()
    }).subscribe({
      next: d => {
        this.resumen.set(d.resumen); this.empleados.set(d.empleados); this.salas.set(d.salas); this.reservas.set(d.reservas);
        this.error.set('');
      },
      error: e => this.error.set(e.message)
    });
  }
}
