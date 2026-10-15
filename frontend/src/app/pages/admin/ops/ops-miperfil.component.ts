import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { MiPerfilOps } from '../../../models/ops.model';
import { prepararFoto } from './ops.imagen';

/**
 * Perfil de la terapeuta: ella misma sube su foto (con su consentimiento) y edita su presentación y su teléfono.
 * Lo que escribe aquí es lo que ve el público en el sitio web. Nombre y especialidad los define el administrador.
 */
@Component({
  selector: 'app-ops-miperfil',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }
    @if (perfil(); as p) {
      <div style="display:grid; grid-template-columns:minmax(220px,300px) 1fr; gap:1.2rem; align-items:start" class="fichas-layout">
        <div class="ops-card" style="text-align:center">
          @if (p.tieneFoto) {
            <img [src]="foto()" alt="Tu foto de perfil" style="width:100%; aspect-ratio:3/4; object-fit:cover; border-radius:14px">
          } @else {
            <div style="width:100%; aspect-ratio:3/4; border-radius:14px; display:grid; place-items:center; background:#130e1a; font-size:4rem; color:var(--ops-muted)">{{ p.nombre.charAt(0) }}</div>
          }
          <input #archivo type="file" accept="image/*" hidden (change)="elegir($event)">
          <div class="ops-row" style="justify-content:center; margin-top:0.8rem">
            <button type="button" class="ops-btn primary" [disabled]="subiendo()" (click)="archivo.click()">{{ subiendo() ? 'Subiendo…' : (p.tieneFoto ? 'Cambiar foto' : 'Subir foto') }}</button>
            @if (p.tieneFoto) { <button type="button" class="ops-btn danger" (click)="quitar()">Quitar</button> }
          </div>
          <p class="ops-sub" style="margin-top:0.8rem">Sube solo una foto tuya. Se verá en el sitio público junto a tu nombre y presentación. Puedes quitarla cuando quieras.</p>
        </div>

        <form class="ops-card" (ngSubmit)="guardar()">
          <h3 style="margin:0 0 0.8rem">{{ p.nombre }}</h3>
          <p class="ops-sub" style="margin-bottom:1rem">{{ p.especialidad || 'Sin especialidad definida' }} · el nombre y la especialidad los define la administración.</p>
          @if (error()) { <div class="ops-banner err">{{ error() }}</div> }
          <div class="form-grid">
            <div class="field"><label for="mp-tel">Teléfono de contacto (interno)</label><input id="mp-tel" class="ops-input" name="telefono" maxlength="20" [(ngModel)]="telefono"></div>
            <div class="field full"><label for="mp-bio">Presentación pública</label>
              <textarea id="mp-bio" class="ops-input" rows="4" name="bio" maxlength="500" [(ngModel)]="bio"></textarea>
              <span class="muted">{{ bio.length }}/500 · es lo que lee el cliente en el sitio.</span></div>
          </div>
          <div class="modal-foot"><button type="submit" class="ops-btn primary">Guardar perfil</button></div>
        </form>
      </div>
    } @else if (!error()) { <div class="ops-empty">Cargando tu perfil…</div> }
    @if (error() && !perfil()) { <div class="ops-banner err">{{ error() }}</div> }
  `
})
export class OpsMiPerfilComponent implements OnInit {
  private admin = inject(AdminApiService);

  perfil = signal<MiPerfilOps | null>(null);
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  error = signal('');
  subiendo = signal(false);
  version = signal(Date.now());
  telefono = '';
  bio = '';

  foto = () => this.admin.urlFoto(this.perfil()!.id, String(this.version()));

  ngOnInit(): void {
    this.admin.miPerfil().subscribe({ next: p => this.asignar(p), error: e => this.error.set(e.message) });
  }

  private asignar(p: MiPerfilOps): void {
    this.perfil.set(p);
    this.telefono = p.telefono ?? '';
    this.bio = p.bio ?? '';
    this.version.set(Date.now());
  }

  private aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  async elegir(evento: Event): Promise<void> {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) { return; }
    this.subiendo.set(true);
    try {
      const blob = await prepararFoto(archivo);
      this.admin.subirMiFoto(blob).subscribe({
        next: p => { this.subiendo.set(false); this.asignar(p); this.aviso('Foto actualizada.'); },
        error: e => { this.subiendo.set(false); this.aviso(e.message, true); }
      });
    } catch (e) {
      this.subiendo.set(false);
      this.aviso((e as Error).message, true);
    }
  }

  quitar(): void {
    if (confirm('¿Quitar tu foto de perfil del sitio?')) {
      this.admin.quitarMiFoto().subscribe({ next: p => { this.asignar(p); this.aviso('Foto eliminada.'); }, error: e => this.aviso(e.message, true) });
    }
  }

  guardar(): void {
    this.error.set('');
    this.admin.actualizarMiPerfil({ telefono: this.telefono, bio: this.bio }).subscribe({
      next: p => { this.asignar(p); this.aviso('Perfil guardado.'); },
      error: e => this.error.set(e.message)
    });
  }
}
