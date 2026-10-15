import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { ReservaOps } from '../../../models/ops.model';
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
          <div class="field">
            <label for="cobro-monto">Monto (S/)</label>
            <input id="cobro-monto" class="ops-input" type="number" min="1" step="0.01" name="monto" [(ngModel)]="monto">
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
  error = '';
  enviando = false;

  get codigo(): string { return codigoReserva(this.reserva.id); }
  get precioTxt(): string { return soles(this.reserva.precio); }

  ngOnInit(): void {
    this.monto = this.reserva.precio;
  }

  cobrar(): void {
    if (!(this.monto > 0)) {
      this.error = 'El monto debe ser mayor que cero.';
      return;
    }
    this.enviando = true;
    this.error = '';
    this.admin.cobrarYConfirmar(this.reserva.id, this.monto, this.metodo).subscribe({
      next: r => { this.enviando = false; this.cobrada.emit(r); },
      error: e => { this.enviando = false; this.error = e.message; }
    });
  }
}
