import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { CanjeValidoOps, ReservaOps } from '../../../models/ops.model';
import { METODOS_PAGO, codigoReserva, soles } from './ops.util';

/** Cobra una reserva en espera: registra el pago y la pasa a CONFIRMADA en una sola transacción del backend. */
@Component({
  selector: 'app-ops-cobro-modal',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    <div class="modal-back" (click)="cerrar.emit()">
      <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="cobrar()">
        <h3>Cobrar y confirmar · {{ codigo }}</h3>
        <p class="ops-sub" style="margin-bottom:1rem">{{ reserva.clienteNombre }} — {{ reserva.servicioNombre }} ({{ precioTxt }})</p>
        @if (error) { <div class="ops-banner err">{{ error }}</div> }
        <div class="form-grid">
          <div class="field full">
            <label for="cobro-canje">Código de canje de puntos (opcional)</label>
            <div class="ops-row">
              <input id="cobro-canje" class="ops-input" style="flex:1" name="canje" [(ngModel)]="codigoCanje" placeholder="CJ-XXXXXX" [disabled]="validado !== null && validado.valido">
              <button type="button" class="ops-btn" (click)="validar()" [disabled]="!codigoCanje.trim() || validando">{{ validando ? 'Validando…' : 'Validar' }}</button>
              @if (validado?.valido) { <button type="button" class="ops-btn sm" (click)="quitarCodigo()">Quitar</button> }
            </div>
            @if (validado; as v) {
              <span [class]="v.valido ? 'badge COMPLETADA' : 'badge CANCELADA'" style="margin-top:0.4rem; align-self:flex-start">
                {{ v.valido ? ('✓ ' + v.titulo + ' · ' + v.cliente + ': se cobrará ' + soles(v.montoACobrar)) : ('✗ ' + v.motivo) }}
              </span>
            }
          </div>
          <div class="field">
            <label for="cobro-monto">Total a cobrar (S/)</label>
            <input id="cobro-monto" class="ops-input" type="number" name="monto" [ngModel]="monto" readonly>
            <span class="muted">Solo el pago completo asegura el turno: el sistema no admite cobros parciales.</span>
          </div>
          <div class="field">
            <label for="cobro-metodo">Método de pago</label>
            <select id="cobro-metodo" class="ops-select" name="metodo" [(ngModel)]="metodo">
              @for (m of metodos; track m) { <option [value]="m">{{ m }}</option> }
            </select>
          </div>
        </div>
        <div class="modal-foot">
          <button type="button" class="ops-btn" (click)="cerrar.emit()">Volver</button>
          <button type="submit" class="ops-btn primary" [disabled]="enviando">{{ enviando ? 'Registrando…' : 'Cobrar y confirmar' }}</button>
        </div>
      </form>
    </div>
  `
})
export class OpsCobroModalComponent {
  private admin = inject(AdminApiService);
  @Input({ required: true }) reserva!: ReservaOps;
  @Output() cerrar = new EventEmitter<void>();
  @Output() cobrada = new EventEmitter<ReservaOps>();

  metodos = METODOS_PAGO;
  metodo = METODOS_PAGO[0];
  monto = 0;
  codigoCanje = '';
  validado: CanjeValidoOps | null = null;
  validando = false;
  soles = soles;
  error = '';
  enviando = false;

  get codigo(): string { return codigoReserva(this.reserva.id); }
  get precioTxt(): string { return soles(this.reserva.precio); }

  ngOnInit(): void {
    this.monto = this.reserva.precio;
  }

  validar(): void {
    this.error = '';
    this.validando = true;
    this.admin.validarCanje(this.codigoCanje, this.reserva.id).subscribe({
      next: v => { this.validando = false; this.validado = v; if (v.valido) { this.monto = v.montoACobrar; } },
      error: e => { this.validando = false; this.validado = null; this.error = e.message; }
    });
  }

  quitarCodigo(): void {
    this.validado = null;
    this.codigoCanje = '';
    this.monto = this.reserva.precio;
  }

  cobrar(): void {
    const conCanje = this.validado?.valido === true;
    this.enviando = true;
    this.error = '';
    this.admin.cobrarYConfirmar(this.reserva.id, this.monto, this.metodo, conCanje ? this.codigoCanje.trim() : undefined).subscribe({
      next: r => { this.enviando = false; this.cobrada.emit(r); },
      error: e => { this.enviando = false; this.error = e.message; }
    });
  }
}
