import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AdminApiService } from '../../../services/admin-api.service';
import { MiCuentaOps, PuntosOps, ReservaOps } from '../../../models/ops.model';
import { ESTADO_TXT, codigoReserva, fechaHoraTxt, soles } from './ops.util';

/**
 * Portal del cliente: solo ve y modifica lo suyo (el backend filtra por la identidad del token).
 * Puntos y nivel se calculan de sus sesiones completadas; la cancelación sigue la política del servidor.
 */
@Component({
  selector: 'app-ops-micuenta',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './ops.scss',
  template: `
    @if (mensaje()) { <div class="ops-banner" [class.ok]="!mensaje()!.error" [class.err]="mensaje()!.error">{{ mensaje()!.texto }}</div> }

    @if (cuenta(); as c) {
      <div class="kpi-grid">
        <div class="kpi"><div class="k-label">Nivel</div><div class="k-value">{{ c.nivel }}</div>
          <div class="k-note">@if (c.siguienteNivel) { Te faltan {{ c.puntosParaSiguiente }} puntos para {{ c.siguienteNivel }} } @else { Nivel máximo alcanzado }</div></div>
        <div class="kpi"><div class="k-label">Saldo de puntos</div><div class="k-value">{{ c.puntos }}</div><div class="k-note">Ganados menos canjeados · detalle abajo</div></div>
        <div class="kpi"><div class="k-label">Sesiones completadas</div><div class="k-value">{{ c.sesionesCompletadas }}</div><div class="k-note">Historial real de tu cuenta</div></div>
        <div class="kpi"><div class="k-label">Cancelaciones del mes</div><div class="k-value">{{ c.cancelacionesMes }} / {{ c.limiteCancelacionesMes }}</div>
          <div class="k-note">Se cancela hasta {{ c.horasMinimasCancelacion }} h antes de la sesión</div></div>
      </div>

      @if (puntos(); as p) {
        <div class="ops-head"><h3 class="ops-title">Programa de puntos</h3><span class="muted">Ganados en total: {{ p.ganados }} · Canjeados: {{ p.canjeados }}</span></div>
        <div class="state-grid">
          <div class="ops-card"><h4 class="ops-title">Cómo funciona</h4>
            <ul style="margin:0.5rem 0 0; padding-left:1.1rem; line-height:1.6">@for (r of p.reglas; track r) { <li>{{ r }}</li> }</ul></div>
          <div class="ops-card"><h4 class="ops-title">Niveles</h4>
            @for (n of p.niveles; track n.nombre) {
              <div class="ops-row" style="justify-content:space-between; padding:0.3rem 0; border-bottom:1px solid var(--ops-line)">
                <span [style.font-weight]="n.actual ? 800 : 400" [style.color]="n.actual ? 'var(--ops-pink)' : ''">{{ n.nombre }}@if (n.actual) { · tu nivel }</span><span class="muted">desde {{ n.desde }} pts</span></div>
            }
            @if (p.siguienteNivel) { <p class="ops-sub" style="margin-top:0.6rem">Te faltan {{ p.puntosParaSiguiente }} puntos ganados para {{ p.siguienteNivel }}.</p> }</div>
        </div>

        <div class="ops-head"><h3 class="ops-title">Canjear puntos</h3><span class="muted">Tienes {{ p.saldo }} puntos disponibles</span></div>
        <div class="state-grid">
          @for (b of p.beneficios; track b.id) {
            <div class="ops-card">
              <h4 style="margin:0">{{ b.titulo }}</h4><p class="ops-sub">{{ b.descripcion }}</p>
              <div class="ops-row" style="justify-content:space-between; margin-top:0.6rem"><strong>{{ b.puntos }} pts</strong>
                <button type="button" class="ops-btn sm primary" [disabled]="!b.alcanza" (click)="canjear(b.id, b.titulo, b.puntos)">{{ b.alcanza ? 'Canjear' : 'Te faltan ' + (b.puntos - p.saldo) }}</button></div>
            </div>
          }
        </div>

        @if (p.canjes.length) {
          <div class="ops-head"><h3 class="ops-title">Mis códigos de canje</h3><span class="muted">Muéstralos en recepción al pagar tu sesión</span></div>
          <div class="ops-table-wrap" style="margin-bottom:1.6rem"><table class="ops-table" style="min-width:560px">
            <thead><tr><th>Código</th><th>Beneficio</th><th>Descuento</th><th>Estado</th></tr></thead>
            <tbody>@for (k of p.canjes; track k.codigo) {
              <tr><td class="mono" style="font-size:1rem; color:var(--ops-text)">{{ k.codigo }}</td><td>{{ k.titulo }}</td><td>{{ soles(k.descuento) }}</td>
                <td><span [class]="k.estado === 'DISPONIBLE' ? 'badge CONFIRMADA' : 'badge COMPLETADA'">{{ k.estado === 'DISPONIBLE' ? 'Disponible' : 'Usado' }}</span></td></tr>
            }</tbody></table></div>
        }

        <div class="ops-head"><h3 class="ops-title">Historial de puntos</h3></div>
        <div class="ops-table-wrap" style="margin-bottom:1.6rem"><table class="ops-table" style="min-width:560px">
          <thead><tr><th>Fecha</th><th>Detalle</th><th style="text-align:right">Puntos</th></tr></thead>
          <tbody>@for (m of p.movimientos; track $index) {
            <tr><td>{{ fechaHora(m.fecha) }}</td><td>{{ m.descripcion }}</td>
              <td style="text-align:right; font-weight:800" [style.color]="m.puntos >= 0 ? 'var(--ops-ok)' : 'var(--ops-bad)'">{{ m.puntos > 0 ? '+' : '' }}{{ m.puntos }}</td></tr>
          } @empty { <tr><td colspan="3" class="ops-empty">Aún no tienes movimientos: tus puntos llegan cuando completes una sesión.</td></tr> }</tbody></table></div>
      }

      <div class="ops-head"><h3 class="ops-title">Próxima cita</h3>
        <button type="button" class="ops-btn primary" (click)="reservar()">+ Reservar nueva cita</button></div>
      @if (c.proximaReserva; as p) {
        <div class="ops-card" style="margin-bottom:1.6rem">
          <div class="ops-row" style="justify-content:space-between">
            <div>
              <strong style="font-size:1.1rem">{{ p.servicioNombre }}</strong>
              <div class="muted">{{ fechaHora(p.fechaHora) }} · con {{ p.empleadoNombre }} · {{ p.salaNombre }}</div>
            </div>
            <span class="badge" [class]="'badge ' + p.estadoOperativo">{{ estadoTxt[p.estadoOperativo] }}</span>
          </div>
          @if (p.estadoOperativo === 'EN_ESPERA') {
            <p class="ops-sub" style="margin-top:0.7rem">Tu reserva espera la confirmación y el cobro en recepción. Si no se confirma a tiempo, expira.</p>
          }
        </div>
      } @else {
        <div class="ops-card ops-empty" style="margin-bottom:1.6rem">No tienes citas próximas.</div>
      }

      <div class="ops-head"><h3 class="ops-title">Mis reservas</h3>
        <div class="ops-row">
          <button type="button" class="ops-chip" [class.on]="filtro() === 'PROXIMAS'" (click)="filtro.set('PROXIMAS')">Próximas</button>
          <button type="button" class="ops-chip" [class.on]="filtro() === 'HISTORIAL'" (click)="filtro.set('HISTORIAL')">Historial</button>
        </div></div>
      <div class="ops-table-wrap" style="margin-bottom:1.6rem">
        <table class="ops-table" style="min-width:760px">
          <thead><tr><th>Código</th><th>Servicio</th><th>Terapeuta</th><th>Cabina</th><th>Horario</th><th>Precio</th><th>Estado</th><th>Acción</th></tr></thead>
          <tbody>
            @for (r of visibles(); track r.id) {
              <tr>
                <td class="mono">{{ codigo(r.id) }}</td><td>{{ r.servicioNombre }}</td><td>{{ r.empleadoNombre }}</td><td>{{ r.salaNombre }}</td>
                <td>{{ fechaHora(r.fechaHora) }}</td><td>{{ soles(r.precio) }}</td>
                <td><span class="badge" [class]="'badge ' + r.estadoOperativo">{{ estadoTxt[r.estadoOperativo] }}</span>
                  @if (r.observacion && r.estado === 'CANCELADA') { <div class="muted">{{ r.observacion }}</div> }</td>
                <td>@if (r.estadoOperativo === 'EN_ESPERA' || r.estadoOperativo === 'CONFIRMADA') {
                      <button type="button" class="ops-btn sm danger" (click)="cancelar(r)">Cancelar</button>
                    } @else { <span class="muted">—</span> }</td>
              </tr>
            } @empty { <tr><td colspan="8" class="ops-empty">No hay reservas en esta lista.</td></tr> }
          </tbody>
        </table>
      </div>

      <div class="ops-head"><h3 class="ops-title">Mis datos</h3></div>
      <form class="ops-card" style="max-width:560px" (ngSubmit)="guardar()">
        @if (errorPerfil()) { <div class="ops-banner err">{{ errorPerfil() }}</div> }
        <div class="form-grid">
          <div class="field full"><label for="mc-correo">Correo (no se puede cambiar)</label><input id="mc-correo" class="ops-input" [value]="c.correo" disabled></div>
          <div class="field"><label for="mc-nombre">Nombre</label><input id="mc-nombre" class="ops-input" name="nombre" [(ngModel)]="nombre" required minlength="3"></div>
          <div class="field"><label for="mc-tel">Teléfono</label><input id="mc-tel" class="ops-input" name="telefono" maxlength="20" [(ngModel)]="telefono"></div>
        </div>
        <div class="modal-foot"><button type="submit" class="ops-btn primary">Guardar cambios</button></div>
      </form>
    } @else if (!error()) {
      <div class="ops-empty">Cargando tu cuenta…</div>
    }
    @if (error()) { <div class="ops-banner err">{{ error() }}</div> }
  `
})
export class OpsMiCuentaComponent implements OnInit {
  private admin = inject(AdminApiService);
  private router = inject(Router);

  cuenta = signal<MiCuentaOps | null>(null);
  puntos = signal<PuntosOps | null>(null);
  reservas = signal<ReservaOps[]>([]);
  filtro = signal<'PROXIMAS' | 'HISTORIAL'>('PROXIMAS');
  mensaje = signal<{ texto: string; error: boolean } | null>(null);
  error = signal('');
  errorPerfil = signal('');
  nombre = '';
  telefono = '';

  readonly estadoTxt = ESTADO_TXT; codigo = codigoReserva; fechaHora = fechaHoraTxt; soles = soles;

  visibles = computed(() => {
    const activa = (r: ReservaOps) => r.estadoOperativo === 'EN_ESPERA' || r.estadoOperativo === 'CONFIRMADA' || r.estadoOperativo === 'EN_SESION';
    const lista = this.reservas().filter(r => this.filtro() === 'PROXIMAS' ? activa(r) : !activa(r));
    return lista.sort((a, b) => (this.filtro() === 'PROXIMAS' ? 1 : -1) * a.fechaHora.localeCompare(b.fechaHora));
  });

  ngOnInit(): void {
    this.cargar();
  }

  private aviso(texto: string, error = false): void {
    this.mensaje.set({ texto, error });
    if (!error) { setTimeout(() => this.mensaje.set(null), 4000); }
  }

  cargar(): void {
    forkJoin({ cuenta: this.admin.miCuenta(), reservas: this.admin.misReservas(), puntos: this.admin.puntos() }).subscribe({
      next: d => {
        this.cuenta.set(d.cuenta); this.reservas.set(d.reservas); this.puntos.set(d.puntos);
        this.nombre = d.cuenta.nombre; this.telefono = d.cuenta.telefono ?? '';
        this.error.set('');
      },
      error: e => this.error.set(e.message)
    });
  }

  canjear(id: string, titulo: string, costo: number): void {
    if (confirm(`¿Canjear ${costo} puntos por "${titulo}"? Recibirás un código de un solo uso para mostrar en recepción.`)) {
      this.admin.canjear(id).subscribe({
        next: c => { this.aviso(`Canje listo. Tu código es ${c.codigo}: muéstralo en recepción al pagar.`); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }

  reservar(): void {
    void this.router.navigate(['/reserva']);
  }

  cancelar(r: ReservaOps): void {
    if (confirm(`¿Cancelar tu reserva ${codigoReserva(r.id)}? Cuenta para tu límite mensual de cancelaciones.`)) {
      this.admin.cancelar(r.id).subscribe({
        next: () => { this.aviso('Reserva cancelada.'); this.cargar(); },
        error: e => this.aviso(e.message, true)
      });
    }
  }

  guardar(): void {
    this.errorPerfil.set('');
    this.admin.actualizarMiCuenta({ nombre: this.nombre, telefono: this.telefono }).subscribe({
      next: c => { this.cuenta.set(c); this.aviso('Datos actualizados.'); },
      error: e => this.errorPerfil.set(e.message)
    });
  }
}
