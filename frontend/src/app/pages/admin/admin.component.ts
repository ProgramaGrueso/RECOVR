import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService, Rol, Vista } from '../../services/auth.service';
import { ServerTimeService } from '../../services/server-time.service';
import { OpsResumenComponent } from './ops/ops-resumen.component';
import { OpsAgendaComponent } from './ops/ops-agenda.component';
import { OpsClientesComponent } from './ops/ops-clientes.component';
import { OpsCajaComponent } from './ops/ops-caja.component';
import { OpsPersonalComponent } from './ops/ops-personal.component';
import { OpsCatalogoComponent } from './ops/ops-catalogo.component';
import { OpsFichasComponent } from './ops/ops-fichas.component';
import { OpsSistemaComponent } from './ops/ops-sistema.component';
import { OpsSeguridadComponent } from './ops/ops-seguridad.component';
import { OpsMiCuentaComponent } from './ops/ops-micuenta.component';
import { OpsUsuariosComponent } from './ops/ops-usuarios.component';
import { OpsMiPerfilComponent } from './ops/ops-miperfil.component';
import { OpsCambiarClaveComponent } from './ops/ops-cambiar-clave.component';

interface NavItem { key: Vista; icon: string; label: string; titulo: string; }

const NAV: NavItem[] = [
  { key: 'RESUMEN', icon: '◈', label: 'Resumen', titulo: 'PANEL DE CONTROL' },
  { key: 'AGENDA', icon: '◷', label: 'Agenda', titulo: 'AGENDA Y CONTROL DE SALAS' },
  { key: 'CLIENTES', icon: '☗', label: 'Clientes', titulo: 'DIRECTORIO DE CLIENTES' },
  { key: 'CAJA', icon: '❖', label: 'Caja', titulo: 'CAJA DEL DÍA' },
  { key: 'PERSONAL', icon: '◌', label: 'Personal', titulo: 'GESTIÓN DE PERSONAL (MASAJISTAS)' },
  { key: 'CATALOGO', icon: '◇', label: 'Catálogo y cabinas', titulo: 'CATÁLOGO DE SERVICIOS Y CABINAS' },
  { key: 'USUARIOS', icon: '☰', label: 'Usuarios y accesos', titulo: 'USUARIOS Y ACCESOS DEL PERSONAL' },
  { key: 'MI_PERFIL', icon: '☺', label: 'Mi perfil', titulo: 'MI PERFIL PÚBLICO' },
  { key: 'FICHAS', icon: '✚', label: 'Fichas de clientes', titulo: 'FICHAS CONFIDENCIALES DE MIS CLIENTES' },
  { key: 'SISTEMA', icon: '⚙', label: 'Sistema y auditoría', titulo: 'ESTADO DEL SISTEMA Y AUDITORÍA' },
  { key: 'SEGURIDAD', icon: '⚑', label: 'Permisos por rol', titulo: 'MATRIZ DE PERMISOS POR ROL' },
  { key: 'MI_CUENTA', icon: '◉', label: 'Mi cuenta', titulo: 'MI CUENTA' }
];

const ROL_TXT: Record<Rol, { titulo: string; alcance: string; tono: string }> = {
  ADMIN: { titulo: 'Administración', alcance: 'Gestión completa del negocio, personal, catálogo, auditoría y seguridad', tono: 'burgundy' },
  RECEPCIONISTA: { titulo: 'Recepción', alcance: 'Agenda, check-in, clientes y caja', tono: 'silver' },
  ESPECIALISTA: { titulo: 'Terapeuta', alcance: 'Tu agenda y las fichas de los clientes que atiendes', tono: 'teal' },
  CLIENTE: { titulo: 'Cliente', alcance: 'Tus reservas, puntos y datos personales', tono: 'light' }
};

/**
 * Zona privada. Cada rol ve solo sus módulos (lista definida en AuthService) y todas las pantallas leen y escriben
 * en la base de datos mediante la API, que vuelve a validar el rol en cada petición.
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    RouterLink, OpsResumenComponent, OpsAgendaComponent, OpsClientesComponent, OpsCajaComponent, OpsPersonalComponent,
    OpsCatalogoComponent, OpsFichasComponent, OpsSistemaComponent, OpsSeguridadComponent, OpsMiCuentaComponent, OpsUsuariosComponent,
    OpsMiPerfilComponent, OpsCambiarClaveComponent
  ],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private serverTimeService = inject(ServerTimeService);
  auth = inject(AuthService);

  activeView: Vista = 'RESUMEN';
  cambiandoClave = false;
  serverTime = '';
  private clockInterval: ReturnType<typeof setInterval> | undefined;

  user = computed(() => this.auth.user()!);
  rolInfo = computed(() => ROL_TXT[this.user().rol]);
  iniciales = computed(() => this.user().nombre.split(' ').filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase());
  navItems = computed(() => NAV.filter(i => this.auth.canAccess(i.key)));

  ngOnInit(): void {
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 1000);
    this.activeView = this.auth.vistaInicial() ?? 'MI_CUENTA';
  }

  ngOnDestroy(): void {
    if (this.clockInterval) { clearInterval(this.clockInterval); }
  }

  /** Con una clave temporal el cambio es obligatorio: el modal no se puede cerrar. */
  get claveObligatoria(): boolean {
    return this.user().debeCambiarClave;
  }

  get titulo(): string {
    return NAV.find(i => i.key === this.activeView)?.titulo ?? '';
  }

  private updateClock(): void {
    this.serverTime = this.serverTimeService.getNowISOString().replace('T', ' ').substring(0, 19);
  }

  setView(view: string): void {
    if (this.auth.canAccess(view)) {
      this.activeView = view as Vista;
    }
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }
}
