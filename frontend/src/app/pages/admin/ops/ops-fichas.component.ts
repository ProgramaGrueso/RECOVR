import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../../services/admin-api.service';
import { FichaForm, FichaOps, MiClienteOps } from '../../../models/ops.model';
import { fechaHoraTxt } from './ops.util';

/**
 * Fichas confidenciales: la terapeuta ve solo a los clientes que atiende y escribe su ficha y sus notas de sesión.
 * El backend rechaza (403) cualquier otro cliente, y ADMIN/RECEPCIONISTA no tienen acceso (privacidad).
 */
@Component({
  selector: 'app-ops-fichas',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }
    <div style="display:grid; grid-template-columns:minmax(240px,320px) 1fr; gap:1rem; align-items:start" class="fichas-layout">
      <div class="ops-card">
        <input class="ops-input" style="width:100%; margin-bottom:0.7rem" type="search" placeholder="Buscar cliente" aria-label="Buscar cliente"
               [ngModel]="texto()" (ngModelChange)="texto.set($event)">
        @for (c of visibles(); track c.clienteId) {
          <button type="button" class="ops-btn" style="display:block; width:100%; text-align:left; margin-bottom:0.4rem"
                  [style.border-color]="seleccion()?.clienteId === c.clienteId ? 'var(--ops-pink)' : ''" (click)="abrir(c)">
            <strong>{{ c.nombre }}</strong>
            <div class="muted">{{ c.sesiones }} sesión(es) @if (c.tieneFicha) { · con ficha } @else { · sin ficha }</div>
            <div class="muted">@if (c.proximaSesion) { Próxima: {{ fechaHora(c.proximaSesion) }} } @else { Sin próxima sesión }</div>
          </button>
        } @empty { <div class="ops-empty">Aún no tienes clientes asignados.</div> }
      </div>

      <div>
        @if (ficha(); as f) {
          <form class="ops-card" style="margin-bottom:1rem" (ngSubmit)="guardar()">
            <div class="ops-head" style="margin-bottom:0.6rem">
              <div><h3 style="margin:0">{{ f.clienteNombre }}</h3>
                <p class="ops-sub">{{ f.telefono || 'Sin teléfono' }} @if (f.actualizadoPor) { · actualizada por {{ f.actualizadoPor }} el {{ fechaHora(f.actualizadoEn) }} }</p></div>
              <button type="submit" class="ops-btn primary">Guardar ficha</button>
            </div>
            @if (errorFicha()) { <div class="ops-banner err">{{ errorFicha() }}</div> }
            <div class="form-grid">
              <div class="field"><label for="fi-pres">Presión preferida</label>
                <select id="fi-pres" class="ops-select" name="presion" [(ngModel)]="form.presionPreferida">
                  <option value="">Sin definir</option><option value="SUAVE">Suave</option><option value="MEDIA">Media</option><option value="ALTA">Alta</option>
                </select></div>
              <div class="field"><label for="fi-ace">Aceites</label><input id="fi-ace" class="ops-input" name="aceites" maxlength="255" [(ngModel)]="form.aceites"></div>
              <div class="field full"><label for="fi-ale">Alergias</label><textarea id="fi-ale" class="ops-input" rows="2" name="alergias" maxlength="1000" [(ngModel)]="form.alergias"></textarea></div>
              <div class="field full"><label for="fi-con">Condiciones médicas</label><textarea id="fi-con" class="ops-input" rows="2" name="condiciones" maxlength="1000" [(ngModel)]="form.condiciones"></textarea></div>
              <div class="field full"><label for="fi-zon">Zonas restringidas</label><input id="fi-zon" class="ops-input" name="zonas" maxlength="500" [(ngModel)]="form.zonasRestringidas"></div>
              <div class="field full"><label for="fi-not">Notas generales</label><textarea id="fi-not" class="ops-input" rows="2" name="notas" maxlength="2000" [(ngModel)]="form.notas"></textarea></div>
            </div>
          </form>

          <form class="ops-card" style="margin-bottom:1rem" (ngSubmit)="agregarNota()">
            <h3 style="margin:0 0 0.7rem">Nueva nota de sesión</h3>
            @if (errorNota()) { <div class="ops-banner err">{{ errorNota() }}</div> }
            <div class="form-grid">
              <div class="field full"><label for="nt-trat">Tratamiento</label><input id="nt-trat" class="ops-input" name="tratamiento" maxlength="200" [(ngModel)]="nota.tratamiento" required></div>
              <div class="field"><label for="nt-obs">Observaciones</label><textarea id="nt-obs" class="ops-input" rows="2" name="observaciones" maxlength="2000" [(ngModel)]="nota.observaciones"></textarea></div>
              <div class="field"><label for="nt-rec">Recomendaciones</label><textarea id="nt-rec" class="ops-input" rows="2" name="recomendaciones" maxlength="2000" [(ngModel)]="nota.recomendaciones"></textarea></div>
            </div>
            <div class="modal-foot"><button type="submit" class="ops-btn primary">Agregar nota</button></div>
          </form>

          <div class="ops-head"><h3 class="ops-title">Historial de notas ({{ f.notasSesion.length }})</h3></div>
          @for (n of f.notasSesion; track n.id) {
            <div class="ops-card" style="margin-bottom:0.6rem">
              <div class="ops-row" style="justify-content:space-between">
                <strong>{{ n.tratamiento }}</strong>
                <span class="muted">{{ fechaHora(n.fecha) }} · {{ n.terapeuta }}</span>
              </div>
              @if (n.observaciones) { <p style="margin:0.4rem 0 0">{{ n.observaciones }}</p> }
              @if (n.recomendaciones) { <p class="muted" style="margin:0.3rem 0 0">Recomendaciones: {{ n.recomendaciones }}</p> }
              @if (n.propia) { <button type="button" class="ops-btn sm danger" style="margin-top:0.5rem" (click)="eliminarNota(n.id)">Eliminar mi nota</button> }
            </div>
          } @empty { <div class="ops-card ops-empty">Todavía no hay notas para este cliente.</div> }
        } @else {
          <div class="ops-card ops-empty">Elige un cliente para ver o completar su ficha.</div>
        }
      </div>
    </div>
  `
})
export class OpsFichasComponent implements OnInit {
  private admin = inject(AdminApiService);

  clientes = signal<MiClienteOps[]>([]);
  seleccion = signal<MiClienteOps | null>(null);
  ficha = signal<FichaOps | null>(null);
  texto = signal('');
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  errorFicha = signal('');
  errorNota = signal('');
  form: FichaForm = this.vacia();
  nota = { tratamiento: '', observaciones: '', recomendaciones: '' };
  fechaHora = fechaHoraTxt;

  visibles = computed(() => {
    const q = this.texto().trim().toLowerCase();
    return this.clientes().filter(c => !q || c.nombre.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.cargar();
  }

  private vacia(): FichaForm {
    return { alergias: '', condiciones: '', presionPreferida: '', aceites: '', zonasRestringidas: '', notas: '' };
  }

  private aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(): void {
    this.admin.misClientes().subscribe({ next: c => this.clientes.set(c), error: e => this.aviso(e.message, true) });
  }

  abrir(c: MiClienteOps): void {
    this.seleccion.set(c);
    this.errorFicha.set(''); this.errorNota.set('');
    this.admin.ficha(c.clienteId).subscribe({ next: f => this.mostrar(f), error: e => this.aviso(e.message, true) });
  }

  private mostrar(f: FichaOps): void {
    this.ficha.set(f);
    this.form = {
      alergias: f.alergias ?? '', condiciones: f.condiciones ?? '', presionPreferida: f.presionPreferida ?? '',
      aceites: f.aceites ?? '', zonasRestringidas: f.zonasRestringidas ?? '', notas: f.notas ?? ''
    };
  }

  guardar(): void {
    const c = this.seleccion();
    if (!c) { return; }
    this.errorFicha.set('');
    this.admin.guardarFicha(c.clienteId, this.form).subscribe({
      next: f => { this.mostrar(f); this.aviso('Ficha guardada.'); this.cargar(); },
      error: e => this.errorFicha.set(e.message)
    });
  }

  agregarNota(): void {
    const c = this.seleccion();
    if (!c) { return; }
    this.errorNota.set('');
    this.admin.agregarNota(c.clienteId, this.nota).subscribe({
      next: f => { this.mostrar(f); this.nota = { tratamiento: '', observaciones: '', recomendaciones: '' }; this.aviso('Nota agregada.'); },
      error: e => this.errorNota.set(e.message)
    });
  }

  eliminarNota(id: number): void {
    const c = this.seleccion();
    if (c && confirm('¿Eliminar esta nota?')) {
      this.admin.eliminarNota(id).subscribe({
        next: () => { this.aviso('Nota eliminada.'); this.abrir(c); },
        error: e => this.aviso(e.message, true)
      });
    }
  }
}
