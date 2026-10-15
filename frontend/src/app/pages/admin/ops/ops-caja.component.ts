import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { ApiService } from '../../../services/api.service';
import { ReservaOps } from '../../../models/ops.model';
import { OpsCobroModalComponent } from './ops-cobro-modal.component';
import { METODOS_PAGO, codigoReserva, fechaInput, horaTxt, soles } from './ops.util';

/** Caja del día: lo cobrado por método y lo que falta por cobrar, sobre las reservas reales. */
@Component({
  selector: 'app-ops-caja',
  standalone: true,
  imports: [FormsModule, OpsCobroModalComponent],
  styleUrl: './ops.scss',
  template: `
    @if (api.session()) {
      @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }
      <div class="ops-head">
        <div class="ops-row"><label for="caja-dia" class="muted">Día de caja</label>
          <input id="caja-dia" class="ops-input" type="date" [ngModel]="dia()" (ngModelChange)="dia.set($event)"></div>
        <button type="button" class="ops-btn" (click)="cargar()">↻ Actualizar</button>
      </div>

      <div class="kpi-grid">
        <div class="kpi"><div class="k-label">Cobrado</div><div class="k-value">{{ soles(totalCobrado()) }}</div><div class="k-note">{{ cobradas().length }} reservas pagadas</div></div>
        <div class="kpi"><div class="k-label">Por cobrar</div><div class="k-value">{{ soles(totalPorCobrar()) }}</div><div class="k-note">{{ porCobrar().length }} reservas en espera</div></div>
        @for (m of porMetodo(); track m.metodo) {
          <div class="kpi"><div class="k-label">{{ m.metodo }}</div><div class="k-value">{{ soles(m.total) }}</div><div class="k-note">{{ m.n }} cobro(s)</div></div>
        }
      </div>

      <div class="ops-head"><h3 class="ops-title">Por cobrar</h3></div>
      <div class="ops-table-wrap" style="margin-bottom:1.6rem">
        <table class="ops-table">
          <thead><tr><th>Código</th><th>Cliente</th><th>Servicio</th><th>Hora</th><th>Monto</th><th>Acción</th></tr></thead>
          <tbody>
            @for (r of porCobrar(); track r.id) {
              <tr><td class="mono">{{ codigo(r.id) }}</td><td><strong>{{ r.clienteNombre }}</strong></td><td>{{ r.servicioNombre }}</td>
                <td>{{ hora(r.fechaHora) }}</td><td>{{ soles(r.precio) }}</td>
                <td><button type="button" class="ops-btn sm ok" (click)="cobrando.set(r)">Cobrar y confirmar</button></td></tr>
            } @empty { <tr><td colspan="6" class="ops-empty">No hay cobros pendientes este día.</td></tr> }
          </tbody>
        </table>
      </div>

      <div class="ops-head"><h3 class="ops-title">Cobrado</h3></div>
      <div class="ops-table-wrap">
        <table class="ops-table">
          <thead><tr><th>Código</th><th>Cliente</th><th>Servicio</th><th>Hora</th><th>Método</th><th>Monto</th></tr></thead>
          <tbody>
            @for (r of cobradas(); track r.id) {
              <tr><td class="mono">{{ codigo(r.id) }}</td><td><strong>{{ r.clienteNombre }}</strong></td><td>{{ r.servicioNombre }}</td>
                <td>{{ hora(r.fechaHora) }}</td><td>{{ r.metodoPago }}</td><td>{{ soles(r.precio) }}</td></tr>
            } @empty { <tr><td colspan="6" class="ops-empty">Aún no hay cobros este día.</td></tr> }
          </tbody>
        </table>
      </div>

      @if (cobrando(); as c) {
        <app-ops-cobro-modal [reserva]="c" (cerrar)="cobrando.set(null)" (cobrada)="cobrando.set(null); aviso('Pago registrado: la reserva quedó CONFIRMADA.'); cargar()" />
      }
    }
  `
})
export class OpsCajaComponent implements OnInit {
  private admin = inject(AdminApiService);
  api = inject(ApiService);

  reservas = signal<ReservaOps[]>([]);
  dia = signal(fechaInput());
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  cobrando = signal<ReservaOps | null>(null);
  codigo = codigoReserva; hora = horaTxt; soles = soles;

  private delDia = computed(() => this.reservas().filter(r => r.fechaHora.startsWith(this.dia())));
  cobradas = computed(() => this.delDia().filter(r => r.pagado && r.estado !== 'CANCELADA').sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)));
  porCobrar = computed(() => this.delDia().filter(r => r.estadoOperativo === 'EN_ESPERA').sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)));
  totalCobrado = computed(() => this.cobradas().reduce((s, r) => s + r.precio, 0));
  totalPorCobrar = computed(() => this.porCobrar().reduce((s, r) => s + r.precio, 0));
  porMetodo = computed(() => METODOS_PAGO.map(m => {
    const lista = this.cobradas().filter(r => r.metodoPago === m);
    return { metodo: m, n: lista.length, total: lista.reduce((s, r) => s + r.precio, 0) };
  }).filter(m => m.n > 0));

  ngOnInit(): void {
    if (this.api.session()) { this.cargar(); }
  }

  aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(): void {
    this.admin.reservas().subscribe({ next: r => this.reservas.set(r), error: e => this.aviso(e.message, true) });
  }
}
