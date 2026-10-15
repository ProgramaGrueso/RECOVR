import { Component, inject, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, AdminRole, AdminUser } from '../../services/auth.service';
import {
  ClinicalRecord,
  SystemAuditLog,
  BenefitItem,
  ClientBookingItem,
  PointTransaction
} from '../../models/rbac.model';
import { ServerTimeService } from '../../services/server-time.service';
import { OpsResumenComponent } from './ops/ops-resumen.component';
import { OpsAgendaComponent } from './ops/ops-agenda.component';
import { OpsClientesComponent } from './ops/ops-clientes.component';
import { OpsCajaComponent } from './ops/ops-caja.component';
import { OpsPersonalComponent } from './ops/ops-personal.component';
import { OpsCatalogoComponent } from './ops/ops-catalogo.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    OpsResumenComponent, OpsAgendaComponent, OpsClientesComponent, OpsCajaComponent, OpsPersonalComponent, OpsCatalogoComponent
  ],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  auth = inject(AuthService);
  private serverTimeService = inject(ServerTimeService);

  activeView = 'RESUMEN';
  toastMessage: string | null = null;
  private toastTimer: any = null;
  serverTime: string = '';
  private clockInterval: any;

  ngOnInit() {
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 1000);

    const currentRole = this.currentUser().role;
    if (!this.auth.canAccess(this.activeView, currentRole)) {
      if (currentRole === 'CLIENTE') {
        this.activeView = 'MI_CUENTA';
      } else if (currentRole === 'ESPECIALISTA') {
        this.activeView = 'AGENDA';
      } else {
        this.activeView = 'RESUMEN';
      }
    }

    this.loadClientData();
  }

  ngOnDestroy() {
    if (this.clockInterval) clearInterval(this.clockInterval);
  }

  updateClock() {
    this.serverTime = this.serverTimeService.getNowISOString().replace('T', ' ').substring(0, 19);
  }

  // Roles disponibles para la barra de cambio rápido en vivo
  allRoles: AdminRole[] = ['SUPER ADMIN', 'ADMINISTRADOR', 'RECEPCIONISTA', 'ESPECIALISTA', 'CLIENTE'];

  // Usuario actual computado desde AuthService
  currentUser = computed(() => this.auth.currentUser()!);
  originalUser = computed(() => this.auth.originalUser());

  // -------------------------------------------------------------
  // MENÚ DE NAVEGACIÓN BASADO EN PERMISOS (RBAC)
  // -------------------------------------------------------------
  allNavItems = [
    { key: 'RESUMEN', icon: '◈', label: 'Resumen' },
    { key: 'AGENDA', icon: '◷', label: 'Agenda' },
    { key: 'PACIENTES', icon: '☗', label: 'Clientes' },
    { key: 'CLINICA', icon: '✚', label: 'Ficha Clínica' },
    { key: 'CAJA', icon: '❖', label: 'Caja' },
    { key: 'PERSONAL', icon: '◌', label: 'Personal' },
    { key: 'CATALOGO', icon: '◇', label: 'Catálogo y Cabinas' },
    { key: 'DEVOPS', icon: '⚙', label: 'DevOps & TI' },
    { key: 'MATRIZ_RBAC', icon: '⚑', label: 'Matriz de Roles' },
    { key: 'MI_CUENTA', icon: '◉', label: 'Mi Cuenta (Portal)' }
  ];

  get allowedNavItems() {
    return this.allNavItems.filter(item => this.auth.canAccess(item.key, this.currentUser().role));
  }

  // -------------------------------------------------------------
  // FICHAS CLÍNICAS (MÓDULO EXCLUSIVO PARA ESPECIALISTAS)
  // -------------------------------------------------------------
  clinicalRecords: ClinicalRecord[] = [
    {
      patientId: 'PAT-001',
      patientName: 'Carlos Mendoza',
      age: 38,
      emergencyContact: '+56 9 8877 6655 (Esposa)',
      anamnesis: 'Paciente masculino, deportista amateur (crossfit y running). Refiere sobrecarga en zona lumbar bilateral tras entrenamiento pesado y contractura marcada en trapecio superior derecho.',
      medicalConditions: ['Hipertensión controlada', 'Hernia discal L4-L5 no quirúrgica'],
      allergies: ['Alergia a esencias de lavanda concentradas'],
      preferredPressure: 'DESCOMPRESIÓN MÁXIMA',
      oilPreference: 'Aceite de almendras puro con extracto neutro de árnica',
      restrictedAreas: ['Cervical alta (manipulación sin tracción forzada)'],
      notesHistory: [
        {
          id: 'NOTE-101',
          date: '2026-08-14',
          specialistName: 'Ely',
          treatment: 'Liberación miofascial lumbo-pélvica profunda + percusión',
          findings: 'Contractura severa en cuadrado lumbar derecho. Buena tolerancia a presión 8/10.',
          painScale: 7,
          recommendations: 'Crioterapia localizada en casa 15 min y estiramiento de psoas.'
        },
        {
          id: 'NOTE-102',
          date: '2026-08-21',
          specialistName: 'Ely',
          treatment: 'Descompresión miofascial y estiramiento activo asistido',
          findings: 'Disminución del tono muscular en 40%. Rango articular de cadera mejorado.',
          painScale: 4,
          recommendations: 'Mantener hidratación y agendar sesión de mantención en 10 días.'
        }
      ]
    },
    {
      patientId: 'PAT-002',
      patientName: 'Ignacio Silva',
      age: 44,
      emergencyContact: '+56 9 1122 3344 (Hermano)',
      anamnesis: 'Paciente corredor de 21k. Consulta por dolor punzante en talón izquierdo al levantarse en las mañanas e hiperqueratosis submetatarsal bilateral.',
      medicalConditions: ['Fascitis plantar incipiente pie izquierdo'],
      allergies: ['Sin alergias conocidas'],
      preferredPressure: 'MEDIA',
      oilPreference: 'Crema podológica con urea 20% y árbol de té',
      restrictedAreas: ['Ninguna'],
      notesHistory: [
        {
          id: 'NOTE-201',
          date: '2026-08-21',
          specialistName: 'Miranda',
          treatment: 'Masaje relajante con descarga de zonas de presión',
          findings: 'Resección de callosidades plantares, corte terapéutico de uñas.',
          painScale: 5,
          recommendations: 'Plantillas de soporte de arco y reposo de impacto por 48 horas.'
        }
      ]
    }
  ];

  selectedPatientIndex = 0;
  newClinicalNote = {
    treatment: '',
    findings: '',
    painScale: 5,
    recommendations: ''
  };

  // -------------------------------------------------------------
  // DEVOPS & INFRAESTRUCTURA (SUPER ADMIN)
  // -------------------------------------------------------------
  devopsMetrics = [
    { name: 'API Stripe Payments', status: 'ONLINE', latency: '112 ms', uptime: '99.98%' },
    { name: 'API WhatsApp Business (Notificaciones)', status: 'ONLINE', latency: '240 ms', uptime: '99.95%' },
    { name: 'Base de Datos PostgreSQL Cloud', status: 'ONLINE', latency: '14 ms', pool: '24/50 conexiones' },
    { name: 'Motor de Reservas Spring Boot', status: 'OPTIMAL', latency: '45 ms', memory: '512 MB / 2 GB' }
  ];

  auditLogs: SystemAuditLog[] = [
    { id: 'LOG-8801', timestamp: '2026-08-21 16:04:12', actor: 'Alexei Rivera', role: 'SUPER ADMIN', action: 'Rotación de credenciales webhook Stripe', ip: '190.161.44.12', severity: 'SECURITY' },
    { id: 'LOG-8802', timestamp: '2026-08-21 15:55:02', actor: 'Camila Morales', role: 'RECEPCIONISTA', action: 'Check-in cliente Carlos Mendoza (REC-908123)', ip: '192.168.1.105', severity: 'INFO' },
    { id: 'LOG-8803', timestamp: '2026-08-21 15:30:19', actor: 'Matías Harb', role: 'ADMINISTRADOR', action: 'Ajuste de comisión especialista Ely a 35%', ip: '190.161.88.90', severity: 'INFO' },
    { id: 'LOG-8804', timestamp: '2026-08-21 14:10:44', actor: 'Ely', role: 'ESPECIALISTA', action: 'Registro de evolución clínica paciente PAT-001', ip: '192.168.1.112', severity: 'INFO' },
    { id: 'LOG-8805', timestamp: '2026-08-21 12:00:00', actor: 'SYSTEM_CRON', role: 'SUPER ADMIN', action: 'Backup automatizado de base de datos exitoso (GCS)', ip: '127.0.0.1', severity: 'INFO' }
  ];

  // -------------------------------------------------------------
  // PORTAL CLIENTE ("MI CUENTA"): PUNTOS, ALARMA E INDEMNIZACIÓN
  // -------------------------------------------------------------
  clientBookings: ClientBookingItem[] = [];
  pointsHistory: PointTransaction[] = [];

  // --- CLIENT DASHBOARD GETTERS ---
  get nextSession(): ClientBookingItem | undefined {
    // Busca la primera reserva futura/confirmada del cliente
    // (Simplificado: tomamos la primera CONFIRMADA del array local)
    return this.clientBookings.find(b => b.status === 'CONFIRMADA');
  }

  get clientMembershipLevel(): string {
    const pts = this.currentUser().points || 0;
    if (pts < 100) return 'NUEVO INGRESO';
    if (pts < 300) return 'MIEMBRO ACTIVO';
    return 'SANCTUM ELITE';
  }

  get clientMembershipDesc(): string {
    const pts = this.currentUser().points || 0;
    if (pts < 100) return 'Acumulando puntos iniciales';
    if (pts < 300) return 'Beneficios estándar';
    return 'Beneficios de recuperación';
  }

  get clientBonos() {
    return this.currentUser().bonos;
  }
  
  redeemableBenefits: BenefitItem[] = [
    {
      id: 'BEN-01',
      title: 'Bebida Isotónica & Aromaterapia',
      pointsCost: 150,
      description: 'Bebida con electrolitos de grado médico y toalla caliente perfumada con cedro y sándalo.',
      icon: '🍶',
      category: 'CORTESÍA'
    },
    {
      id: 'BEN-02',
      title: '20% OFF en Masaje Deportivo',
      pointsCost: 300,
      description: 'Descuento directo en tu siguiente sesión de recuperación muscular profunda.',
      icon: '⚡',
      category: 'DESCUENTO'
    },
    {
      id: 'BEN-03',
      title: 'Sesión de Descompresión Gratis',
      pointsCost: 500,
      description: '¡Meta Principal! Sesión completa de 60 minutos de descompresión miofascial 100% gratuita.',
      icon: '🏆',
      category: 'GRATIS'
    },
    {
      id: 'BEN-04',
      title: 'Upgrade a Cabina VIP Hidromasaje',
      pointsCost: 800,
      description: 'Acceso a la suite privada con camilla hidráulica, manta térmica infrarroja y Theragun Pro.',
      icon: '👑',
      category: 'VIP'
    }
  ];

  // Modal de Cancelación con Advertencia e Indemnización
  showCancelModal = false;
  selectedBookingToCancel: ClientBookingItem | null = null;
  cancelWarningData = {
    isCritical: false,
    hoursRemaining: 0,
    fee: 0,
    deadline: '',
    cancellationsUsed: 0,
    isMaxReached: false
  };

  clientPortalData = {
    name: 'Carlos Mendoza',
    memberTier: 'SANCTUM ELITE',
    activePack: {
      title: 'Pack Recuperación Masculina Alto Rendimiento',
      used: 2,
      total: 5,
      remaining: 3,
      expires: '31 Diciembre 2026'
    },
    preferences: {
      pressure: 'Presión Alta / Descompresión Profunda',
      music: 'Frecuencias bajas / Ambiente neutro',
      aroma: 'Cedro y sándalo negro',
      sensitiveAreas: 'Cervical alta con cuidado'
    }
  };

  loadClientData(): void {
    if (this.currentUser().role === 'CLIENTE') {
      this.clientBookings = this.auth.getClientBookings();
      this.pointsHistory = this.auth.getPointsHistory();
    }
  }

  get clientPoints(): number {
    return this.auth.getClientPoints();
  }

  get clientCancellations(): number {
    return this.auth.getClientCancellations();
  }

  get activeNextBooking(): ClientBookingItem | undefined {
    return this.clientBookings.find(b => b.status === 'CONFIRMADA' || b.status === 'EN ESPERA');
  }

  get nextBookingCountdown(): { label: string; isCritical: boolean; hours: number; deadline: string } {
    const booking = this.activeNextBooking;
    if (!booking) {
      return { label: 'Sin sesiones activas programadas', isCritical: false, hours: 99, deadline: this.serverTimeService.getNowISOString().substring(0, 10) };
    }

    const { canCancelFree, hoursRemaining, deadlineTime } = this.auth.canCancelWithoutIndemnity(booking);

    let label = '';
    if (hoursRemaining < 1) {
      label = `En ${Math.round(hoursRemaining * 60)} minutos`;
    } else {
      label = `En ${hoursRemaining} horas`;
    }

    return {
      label,
      isCritical: !canCancelFree,
      hours: hoursRemaining,
      deadline: deadlineTime
    };
  }

  redeemBenefit(benefit: BenefitItem): void {
    const res = this.auth.redeemClientBenefit(benefit.title, benefit.pointsCost);
    this.showToast(res.message);
    if (res.success) {
      this.pointsHistory = this.auth.getPointsHistory();
    }
  }

  openCancelModal(booking: ClientBookingItem): void {
    this.selectedBookingToCancel = booking;
    const cancellations = this.clientCancellations;
    const isMaxReached = cancellations >= 2;
    const { canCancelFree, hoursRemaining, deadlineTime } = this.auth.canCancelWithoutIndemnity(booking);
    const fee = Math.round(booking.price * 0.5);

    this.cancelWarningData = {
      isCritical: !canCancelFree,
      hoursRemaining,
      fee,
      deadline: deadlineTime,
      cancellationsUsed: cancellations,
      isMaxReached
    };

    this.showCancelModal = true;
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
    this.selectedBookingToCancel = null;
  }

  confirmCancellation(): void {
    if (!this.selectedBookingToCancel) return;

    const res = this.auth.cancelClientBooking(this.selectedBookingToCancel.id, true);
    this.showToast(res.message);
    this.closeCancelModal();
    this.loadClientData();
  }

  // -------------------------------------------------------------
  // MATRIZ OFICIAL DE PERMISOS RBAC
  // -------------------------------------------------------------
  rbacRolesList: { name: AdminRole; detail: string; tone: string }[] = [
    { name: 'SUPER ADMIN', detail: 'DUEÑO / TI', tone: 'gold' },
    { name: 'ADMINISTRADOR', detail: 'GERENCIA / DUEÑO', tone: 'burgundy' },
    { name: 'RECEPCIONISTA', detail: 'FRONT-DESK / CAJA', tone: 'silver' },
    { name: 'ESPECIALISTA', detail: 'KINESIÓLOGA / PODÓLOGO', tone: 'teal' },
    { name: 'CLIENTE', detail: 'USUARIO / PACIENTE', tone: 'light' }
  ];

  rbacMatrix = [
    { action: 'Configuración de Servidor, DB y APIs (Stripe, WhatsApp)', values: ['TOTAL', 'DENEGADO', 'DENEGADO', 'DENEGADO', 'DENEGADO'] },
    { action: 'Gestión Global de Cuentas y Asignación de Roles', values: ['TOTAL', 'PERSONAL', 'DENEGADO', 'DENEGADO', 'DENEGADO'] },
    { action: 'Dashboard Financiero, Reportes y KPIs Globales', values: ['TOTAL', 'TOTAL', 'DENEGADO', 'DENEGADO', 'DENEGADO'] },
    { action: 'Crear y Modificar Catálogo de Protocolos y Tarifas', values: ['TOTAL', 'TOTAL', 'DENEGADO', 'DENEGADO', 'DENEGADO'] },
    { action: 'Gestión de Turnos, Comisiones y Horarios de Personal', values: ['TOTAL', 'TOTAL', 'DENEGADO', 'DENEGADO', 'DENEGADO'] },
    { action: 'Ver Agenda General de Todas las Salas y Terapeutas', values: ['LECTURA', 'COMPLETO', 'TIEMPO REAL', 'DENEGADO', 'DENEGADO'] },
    { action: 'Check-in de Pacientes y Cobro en Caja (Punto de Venta)', values: ['AUDITORÍA', 'COMPLETO', 'OPERATIVO', 'DENEGADO', 'DENEGADO'] },
    { action: 'Agendar Citas Manuales (Walk-in / Telefónicas)', values: ['—', 'HABILITADO', 'PRINCIPAL', 'DENEGADO', 'DENEGADO'] },
    { action: 'Gestión de Agenda Propia y Bloqueos de Descanso', values: ['—', 'HABILITADO', 'HABILITADO', 'EXCLUSIVO', 'DENEGADO'] },
    { action: 'Ficha Clínica: Escribir Diagnóstico y Evolución Terapéutica', values: ['PRIVACIDAD', 'PRIVACIDAD', 'DENEGADO', 'CLÍNICO', 'DENEGADO'] },
    { action: 'Ficha Clínica: Ver Historial Confidencial del Paciente', values: ['PRIVACIDAD', 'PRIVACIDAD', 'DENEGADO', 'COMPLETO', 'FICHA PROPIA'] },
    { action: 'Auto-Reserva de Sesiones Online 24/7 (Cliente)', values: ['—', '—', '—', '—', 'PORTAL WEB'] },
    { action: 'Reprogramar o Cancelar Cita Propia', values: ['—', 'SIEMPRE', 'SIEMPRE', 'DENEGADO', 'CON POLÍTICA'] },
    { action: 'Historial Personal de Sesiones, Facturación y Bonos', values: ['—', '—', '—', '—', 'MI CUENTA'] }
  ];

  // -------------------------------------------------------------
  // MÉTODOS DE CONTROL Y NAVEGACIÓN
  // -------------------------------------------------------------
  setView(view: string): void {
    if (this.auth.canAccess(view, this.currentUser().role)) {
      this.activeView = view;
    } else {
      this.showToast(`El rol ${this.currentUser().role} no posee permiso para acceder al módulo ${view}.`);
    }
  }

  onRoleChange(newRole: AdminRole): void {
    this.auth.switchRole(newRole);
    // Si la vista activa no está permitida para el nuevo rol, redirigir a una adecuada
    if (!this.auth.canAccess(this.activeView, newRole)) {
      if (newRole === 'CLIENTE') {
        this.activeView = 'MI_CUENTA';
      } else if (newRole === 'ESPECIALISTA') {
        this.activeView = 'AGENDA';
      } else {
        this.activeView = 'RESUMEN';
      }
    }
    this.showToast(`Rol activo cambiado a: [ ${newRole} ] (${this.currentUser().roleTitle})`);
  }

  restoreOriginalRole(): void {
    this.auth.restoreOriginalRole();
    this.activeView = 'RESUMEN';
    this.showToast(`Restaurado rol original: [ ${this.currentUser().role} ]`);
  }

  logout(): void {
    const role = this.currentUser().role;
    this.auth.logout();
    if (role === 'CLIENTE') {
      this.router.navigate(['/admin/login']);
    } else {
      this.router.navigate(['/admin/login'], { queryParams: { mode: 'staff' } });
    }
  }

  showToast(message: string): void {
    this.toastMessage = message;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.toastMessage = null;
    }, 3800);
  }

  // -------------------------------------------------------------
  // OPERACIONES DE FICHA CLÍNICA
  // -------------------------------------------------------------
  get currentPatient(): ClinicalRecord {
    return this.clinicalRecords[this.selectedPatientIndex] || this.clinicalRecords[0];
  }

  selectPatient(index: number): void {
    this.selectedPatientIndex = index;
  }

  addClinicalEvolution(): void {
    if (!this.newClinicalNote.treatment || !this.newClinicalNote.findings) {
      this.showToast('Debes ingresar el tratamiento aplicado y los hallazgos clínicos.');
      return;
    }

    const note = {
      id: `NOTE-${Math.floor(100 + Math.random() * 900)}`,
      date: this.serverTimeService.getNowISOString().substring(0, 10),
      specialistName: this.currentUser().name,
      treatment: this.newClinicalNote.treatment,
      findings: this.newClinicalNote.findings,
      painScale: this.newClinicalNote.painScale,
      recommendations: this.newClinicalNote.recommendations || 'Seguimiento en próxima sesión.'
    };

    this.currentPatient.notesHistory.unshift(note);
    this.newClinicalNote = { treatment: '', findings: '', painScale: 5, recommendations: '' };
    this.showToast(`Nota clínica registrada exitosamente para ${this.currentPatient.patientName}`);
  }

  deleteClinicalNote(noteId: string): void {
    if (this.currentUser().role === 'ESPECIALISTA' || this.currentUser().role === 'SUPER ADMIN') {
      if (confirm('¿Confirmas eliminar esta nota clínica? Esta acción dejará un registro de auditoría.')) {
        this.currentPatient.notesHistory = this.currentPatient.notesHistory.filter(n => n.id !== noteId);
        this.showToast(`Evolución clínica eliminada del historial a las ${this.serverTime}.`);
      }
    } else {
      this.showToast('Acceso denegado: Privacidad médica. No puedes alterar fichas.');
    }
  }

  // -------------------------------------------------------------
  // OPERACIONES DE DEVOPS & INFRAESTRUCTURA
  // -------------------------------------------------------------
  triggerManualBackup(): void {
    const now = this.serverTimeService.getNowISOString().replace('T', ' ').substring(0, 19);
    this.auditLogs.unshift({
      id: `LOG-${Math.floor(1000 + Math.random() * 9000)}`,
      timestamp: now,
      actor: this.currentUser().name,
      role: 'SUPER ADMIN',
      action: 'Generación manual de snapshot y backup SQL (PostgreSQL Dump -> GCS Bucket)',
      ip: '190.161.44.12',
      severity: 'SECURITY'
    });
    this.showToast('Backup manual ejecutado con éxito. Snapshot almacenado en Google Cloud Storage.');
  }
}
