import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { AuthService } from '../../../services/auth.service';

/**
 * Cambio de contraseña del usuario que inició sesión. Con una clave temporal (cuenta nueva o restablecida) es
 * obligatorio: el modal no se puede cerrar y el servidor bloquea todo lo demás hasta que se cambie.
 */
@Component({
  selector: 'app-ops-cambiar-clave',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    <div class="modal-back" (click)="forzado ? null : cerrar.emit()">
      <form class="modal" (click)="$event.stopPropagation()" (ngSubmit)="cambiar()" autocomplete="off">
        <h3>{{ forzado ? 'Define tu contraseña personal' : 'Cambiar contraseña' }}</h3>
        @if (forzado) {
          <p class="ops-sub" style="margin-bottom:1rem">Ingresaste con una contraseña temporal. Por seguridad debes reemplazarla por una propia antes de continuar.</p>
        }
        @if (error) { <div class="ops-banner err" role="alert">{{ error }}</div> }
        <div class="form-grid">
          <div class="field full"><label for="cc-actual">{{ forzado ? 'Contraseña temporal' : 'Contraseña actual' }}</label>
            <input id="cc-actual" class="ops-input" type="password" name="actual" [(ngModel)]="actual" autocomplete="current-password" required></div>
          <div class="field full"><label for="cc-nueva">Contraseña nueva</label>
            <input id="cc-nueva" class="ops-input" type="password" name="nueva" [(ngModel)]="nueva" autocomplete="new-password" required placeholder="Mínimo 8 caracteres, con letras y números"></div>
          <div class="field full"><label for="cc-conf">Repite la contraseña nueva</label>
            <input id="cc-conf" class="ops-input" type="password" name="confirmar" [(ngModel)]="confirmar" autocomplete="new-password" required></div>
        </div>
        <div class="modal-foot">
          @if (forzado) { <button type="button" class="ops-btn" (click)="salir.emit()">Cerrar sesión</button> }
          @else { <button type="button" class="ops-btn" (click)="cerrar.emit()">Cancelar</button> }
          <button type="submit" class="ops-btn primary" [disabled]="enviando">{{ enviando ? 'Guardando…' : 'Guardar contraseña' }}</button>
        </div>
      </form>
    </div>
  `
})
export class OpsCambiarClaveComponent {
  private admin = inject(AdminApiService);
  private auth = inject(AuthService);

  @Input() forzado = false;
  @Output() cerrar = new EventEmitter<void>();
  @Output() salir = new EventEmitter<void>();

  actual = '';
  nueva = '';
  confirmar = '';
  error = '';
  enviando = false;

  cambiar(): void {
    this.error = '';
    if (this.nueva.length < 8 || !/[A-Za-z]/.test(this.nueva) || !/\d/.test(this.nueva)) {
      this.error = 'La contraseña nueva debe tener al menos 8 caracteres, con letras y números.';
      return;
    }
    if (this.nueva !== this.confirmar) {
      this.error = 'Las contraseñas nuevas no coinciden.';
      return;
    }
    this.enviando = true;
    this.admin.cambiarClave(this.actual, this.nueva).subscribe({
      next: () => this.auth.refrescar().subscribe(() => { this.enviando = false; this.cerrar.emit(); }),
      error: e => { this.enviando = false; this.error = e.message; }
    });
  }
}
