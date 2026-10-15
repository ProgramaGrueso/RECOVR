import { Component, EventEmitter, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../services/api.service';

/**
 * Conexión del panel con el backend. Las pantallas reales (agenda, personal, catálogo...) leen y escriben en
 * MySQL a través de la API, así que necesitan un token JWT. Aquí el personal inicia sesión con su cuenta del servidor;
 * una vez conectado muestra una franja con el rol real que tiene en la API.
 */
@Component({
  selector: 'app-ops-gate',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (api.session(); as s) {
      <div class="ops-row" style="margin-bottom:1rem; font-size:0.8rem; color:var(--ops-muted)">
        <span class="badge COMPLETADA">● Conectado al servidor</span>
        <span>{{ s.correo || 'sesión activa' }} · rol <strong style="color:var(--ops-text)">{{ s.role || '—' }}</strong></span>
        <button type="button" class="ops-btn sm" (click)="desconectar()">Desconectar</button>
      </div>
    } @else {
      <form class="ops-card" style="max-width:520px; margin:1rem 0 2rem" (ngSubmit)="conectar()">
        <h3 style="margin:0 0 0.4rem">Conéctate al servidor</h3>
        <p class="ops-sub" style="margin-bottom:1rem">
          Esta sección trabaja con los datos reales de la base de datos. Inicia sesión con una cuenta del personal
          (ADMIN o RECEPCIONISTA) del backend.
        </p>
        @if (error) { <div class="ops-banner err">{{ error }}</div> }
        <div class="field" style="margin-bottom:0.8rem">
          <label for="gate-correo">Correo</label>
          <input id="gate-correo" class="ops-input" name="correo" [(ngModel)]="correo" placeholder="admin.demo@recovr.test" autocomplete="username">
        </div>
        <div class="field" style="margin-bottom:1rem">
          <label for="gate-pass">Contraseña</label>
          <input id="gate-pass" class="ops-input" type="password" name="password" [(ngModel)]="password" autocomplete="current-password">
        </div>
        <button class="ops-btn primary" type="submit" [disabled]="cargando">{{ cargando ? 'Conectando…' : 'Conectar' }}</button>
        <p class="ops-sub" style="margin-top:0.9rem">
          Con <code>run-demo</code> existen admin.demo@recovr.test y recepcion.demo@recovr.test; la contraseña es la que
          definiste en DEMO_PASSWORD.
        </p>
      </form>
    }
  `
})
export class OpsGateComponent {
  api = inject(ApiService);
  @Output() connected = new EventEmitter<void>();

  correo = '';
  password = '';
  error = '';
  cargando = false;

  conectar(): void {
    if (!this.correo.trim() || !this.password) {
      this.error = 'Ingresa tu correo y contraseña.';
      return;
    }
    this.cargando = true;
    this.error = '';
    this.api.loginConMensaje(this.correo.trim(), this.password).subscribe(res => {
      this.cargando = false;
      if (res.ok) {
        this.password = '';
        this.connected.emit();
      } else {
        this.error = res.message ?? 'No se pudo conectar.';
      }
    });
  }

  desconectar(): void {
    this.api.clearSession();
  }
}
