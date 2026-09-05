import { Component, inject, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, AdminRole, AdminUser } from '../../services/auth.service';
import {
  BookingAdminItem,
  BookingStatus,
  ClinicalRecord,
  CashTransaction,
  StaffMember,
  CabinRoom,
  SystemAuditLog,
  BenefitItem,
  ClientBookingItem,
  PointTransaction
} from '../../models/rbac.model';
import { ServerTimeService } from '../../services/server-time.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
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
    { key: 'AGENDA', icon: '◷', label: 'Agenda & Salas' },
    { key: 'PACIENTES', icon: '☗', label: 'Directorio Pacientes' },
    { key: 'CLINICA', icon: '✚', label: 'Ficha Clínica' },
    { key: 'CAJA', icon: '❖', label: 'Caja & POS' },
    { key: 'PERSONAL', icon: '◌', label: 'Personal & Turnos' },
    { key: 'CATALOGO', icon: '◇', label: 'Catálogo & Salas' },
    { key: 'DEVOPS', icon: '⚙', label: 'DevOps & TI' },
    { key: 'MATRIZ_RBAC', icon: '⚑', label: 'Matriz de Roles' },
    { key: 'MI_CUENTA', icon: '◉', label: 'Mi Cuenta (Portal)' }
  ];

  get allowedNavItems() {
    return this.allNavItems.filter(item => this.auth.canAccess(item.key, this.currentUser().role));
  }

  get allRegisteredClients() {
    return this.auth.getRegisteredClients();
  }

  editingClient: any = null;

  editClient(clientId: string) {
    const clients = this.auth.getRegisteredClients();
    const client = clients.find(c => c.id === clientId);
    if (!client) return;
    
    // Create a copy to edit without affecting the original until saved
    this.editingClient = { ...client };
  }

  saveClientEdit() {
    if (!this.editingClient) return;
    
    const confirmSave = confirm("¿Estás seguro de que deseas guardar los cambios de este perfil?");
    if (!confirmSave) return;
    
    // Convert points to number if needed
    this.editingClient.points = parseInt(this.editingClient.points, 10) || 0;
    
    this.auth.updateClient(this.editingClient);
    
    this.toastMessage = `Cliente ${this.editingClient.id} actualizado exitosamente.`;
    this.editingClient = null; // Close modal
    
    setTimeout(() => this.toastMessage = null, 3000);
  }

  closeEditModal() {
    this.editingClient = null;
  }

  deleteClient(clientId: string) {
    if (confirm(`¿Confirmas que deseas eliminar permanentemente el cliente con ID ${clientId}?`)) {
      this.auth.deleteClient(clientId);
      this.toastMessage = `Cliente ${clientId} eliminado del sistema.`;
      setTimeout(() => this.toastMessage = null, 3000);
    }
  }

  // -------------------------------------------------------------
  // DATOS OPERATIVOS: RESERVAS Y TURNOS
  // -------------------------------------------------------------
  selectedRoomFilter = 'TODAS';
  bookings: BookingAdminItem[] = [
    {
      id: 'REC-908123',
      client: 'Carlos Mendoza',
      clientPhone: '+56 9 8765 4321',
      service: 'Descompresión Miofascial',
      serviceCategory: 'MASAJE',
      prof: 'Valentina Ross',
      room: 'Sanctum 01 (Descompresión)',
      date: '2026-08-21',
      time: '16:00',
      price: 65000,
      status: 'CONFIRMADA',
      paymentStatus: 'PAGADO',
      paymentMethod: 'STRIPE WEB',
      notes: 'Llega 10 minutos antes para cambio de ropa.'
    },
    {
      id: 'REC-871234',
      client: 'Ignacio Silva',
      clientPhone: '+56 9 9123 4567',
      service: 'Podología Clínica Deportiva',
      serviceCategory: 'PODOLOGÍA',
      prof: 'Elena Roth',
      room: 'Sala Podología Clínica',
      date: '2026-08-21',
      time: '17:30',
      price: 45000,
      status: 'EN ESPERA',
      paymentStatus: 'PENDIENTE',
      notes: 'Presenta molestia en talón izquierdo por trote.'
    },
    {
      id: 'REC-761928',
      client: 'Esteban Paz',
      clientPhone: '+56 9 6543 2198',
      service: 'Masaje Terapéutico Deportivo',
      serviceCategory: 'MASAJE',
      prof: 'Astrid Vane',
      room: 'Sanctum 02 (Masoterapia)',
      date: '2026-08-22',
      time: '09:00',
      price: 58000,
      status: 'EN SESIÓN',
      paymentStatus: 'PAGADO',
      paymentMethod: 'POS TRANSBANK',
      notes: 'Sesión enfocada en isquiotibiales y tren inferior.'
    },
    {
      id: 'REC-654129',
      client: 'Mariano Torres',
      clientPhone: '+56 9 7890 1234',
      service: 'Perfilado Podológico Preventivo',
      serviceCategory: 'PODOLOGÍA',
      prof: 'Elena Roth',
      room: 'Sala Podología Clínica',
      date: '2026-08-22',
      time: '10:30',
      price: 40000,
      status: 'COMPLETADA',
      paymentStatus: 'PAGADO',
      paymentMethod: 'EFECTIVO'
    },
    {
      id: 'REC-543210',
      client: 'Felipe Araneda',
      clientPhone: '+56 9 4321 0987',
      service: 'Ritual Sanctum Completo',
      serviceCategory: 'RITUAL',
      prof: 'Valentina Ross',
      room: 'Sanctum 01 (Descompresión)',
      date: '2026-08-22',
      time: '12:00',
      price: 95000,
      status: 'CONFIRMADA',
      paymentStatus: 'PAGADO',
      paymentMethod: 'STRIPE WEB',
      notes: 'Pack 90 minutos con crioterapia localizada.'
    }
  ];

  // Modal para nueva reserva manual (Walk-in / Recepción)
  showManualBookingModal = false;
  newBooking: Partial<BookingAdminItem> = {
    client: '',
    clientPhone: '+56 9 ',
    service: 'Descompresión Miofascial',
    serviceCategory: 'MASAJE',
    prof: 'Valentina Ross',
    room: 'Sanctum 01 (Descompresión)',
    date: '2026-08-22',
    time: '14:00',
    price: 65000,
    status: 'CONFIRMADA',
    paymentStatus: 'PENDIENTE',
    paymentMethod: 'POS TRANSBANK'
  };

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
          specialistName: 'Valentina Ross',
          treatment: 'Liberación miofascial lumbo-pélvica profunda + percusión',
          findings: 'Contractura severa en cuadrado lumbar derecho. Buena tolerancia a presión 8/10.',
          painScale: 7,
          recommendations: 'Crioterapia localizada en casa 15 min y estiramiento de psoas.'
        },
        {
          id: 'NOTE-102',
          date: '2026-08-21',
          specialistName: 'Valentina Ross',
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
          specialistName: 'Elena Roth',
          treatment: 'Podología clínica profiláctica + descarga de zonas de presión',
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
  // CAJA CHICA Y PUNTO DE VENTA (RECEPCIÓN & GERENCIA)
  // -------------------------------------------------------------
  cashRegisterInitial = 120000;
  cashTransactions: CashTransaction[] = [
    {
      id: 'TRX-101',
      time: '09:45',
      client: 'Esteban Paz',
      service: 'Masaje Deportivo 60m',
      amount: 58000,
      method: 'POS TRANSBANK',
      voucherCode: 'VOUCH-88129',
      processedBy: 'Camila Morales'
    },
    {
      id: 'TRX-102',
      time: '11:15',
      client: 'Mariano Torres',
      service: 'Perfilado Podológico',
      amount: 40000,
      method: 'EFECTIVO',
      voucherCode: 'VOUCH-88130',
      processedBy: 'Camila Morales'
    },
    {
      id: 'TRX-103',
      time: '13:30',
      client: 'Rodrigo Vega',
      service: 'Pack 4 Sesiones Descompresión',
      amount: 220000,
      method: 'POS TRANSBANK',
      voucherCode: 'VOUCH-88131',
      processedBy: 'Camila Morales'
    }
  ];

  newPayment = {
    client: '',
    service: 'Descompresión Miofascial',
    amount: 65000,
    method: 'POS TRANSBANK' as const
  };

  // -------------------------------------------------------------
  // PERSONAL Y TURNOS (ADMINISTRADOR / GERENCIA)
  // -------------------------------------------------------------
  staffList: StaffMember[] = [
    {
      id: 'STF-01',
      name: 'Valentina Ross',
      role: 'ESPECIALISTA',
      specialty: 'Kinesióloga & Masoterapeuta Deportiva',
      email: 'valentina@recovr.cl',
      phone: '+56 9 7711 2233',
      shift: 'Turno Tarde (14:00 - 21:00)',
      commissionRate: 35,
      monthlySessions: 46,
      status: 'ACTIVO'
    },
    {
      id: 'STF-02',
      name: 'Elena Roth',
      role: 'ESPECIALISTA',
      specialty: 'Podóloga Clínica Especialista',
      email: 'elena@recovr.cl',
      phone: '+56 9 6622 3344',
      shift: 'Turno Mañana (09:00 - 15:00)',
      commissionRate: 38,
      monthlySessions: 39,
      status: 'ACTIVO'
    },
    {
      id: 'STF-03',
      name: 'Astrid Vane',
      role: 'ESPECIALISTA',
      specialty: 'Masoterapeuta & Recuperación Miofascial',
      email: 'gabriel@recovr.cl',
      phone: '+56 9 5533 4455',
      shift: 'Turno Completo (10:00 - 19:00)',
      commissionRate: 32,
      monthlySessions: 28,
      status: 'ACTIVO'
    },
    {
      id: 'STF-04',
      name: 'Camila Morales',
      role: 'RECEPCIONISTA',
      specialty: 'Front-Desk & Coordinación Operativa',
      email: 'recepcion@recovr.cl',
      phone: '+56 9 4455 6677',
      shift: 'Rotativo (08:30 - 17:30)',
      commissionRate: 5,
      monthlySessions: 0,
      status: 'ACTIVO'
    }
  ];

  deleteStaff(staffId: string): void {
    const role = this.currentUser().role;
    if (role === 'SUPER ADMIN' || role === 'ADMINISTRADOR') {
      if (confirm('¿Estás seguro/a que deseas dar de baja a este colaborador permanentemente?')) {
        this.staffList = this.staffList.filter(s => s.id !== staffId);
        this.showToast(`Colaborador dado de baja del sistema a las ${this.serverTime}.`);
      }
    } else {
      this.showToast('Acceso denegado: No tienes permisos para eliminar personal.');
    }
  }

  protocolStatuses: Record<string, boolean> = {
    'Descompresión Miofascial': true,
    'Masaje Deportivo Restaurativo': true,
    'Podología Clínica Avanzada': true,
    'Ritual Sanctum Completo': true
  };

  protocols = [
    { name: 'Descompresión Miofascial', price: 65000, category: 'MASAJE TERAPÉUTICO', duration: 60, desc: 'Tratamiento profundo con ventosas y percusión mecánica para sobrecarga lumbar y escapular.' },
    { name: 'Masaje Deportivo Restaurativo', price: 58000, category: 'MASAJE TERAPÉUTICO', duration: 60, desc: 'Enfocado en drenaje linfático, retorno venoso y alivio de ácido láctico en tren inferior.' },
    { name: 'Podología Clínica Avanzada', price: 45000, category: 'PODOLOGÍA CLÍNICA', duration: 45, desc: 'Corte profiláctico, desbastado de callosidades, tratamiento de fascitis y asepsia profunda.' },
    { name: 'Ritual Sanctum Completo', price: 95000, category: 'RITUAL COMPLETO', duration: 90, desc: 'Descompresión miofascial 60m + Podología clínica deportiva 30m + Aromaterapia sensorial.' }
  ];

  deleteProtocol(name: string): void {
    const role = this.currentUser().role;
    if (role === 'SUPER ADMIN' || role === 'ADMINISTRADOR') {
      if (confirm(`¿Estás seguro/a que deseas eliminar el protocolo "${name}" del catálogo?`)) {
        this.protocols = this.protocols.filter(p => p.name !== name);
        delete this.protocolStatuses[name];
        this.showToast(`Protocolo ${name} eliminado a las ${this.serverTime}.`);
      }
    } else {
      this.showToast('Acceso denegado: No tienes permisos para eliminar protocolos.');
    }
  }

  // -------------------------------------------------------------
  // SALAS Y CABINAS SANCTUM
  // -------------------------------------------------------------
  cabins: CabinRoom[] = [
    {
      id: 'CAB-01',
      name: 'Sanctum 01 (Descompresión)',
      code: 'S-01',
      type: 'Cabina de Descompresión & Masoterapia Avanzada',
      equipment: ['Camilla hidráulica de alta resistencia', 'Manta térmica infrarroja', 'Equipo de percusión Theragun Pro'],
      status: 'OCUPADA',
      currentTherapist: 'Valentina Ross'
    },
    {
      id: 'CAB-02',
      name: 'Sanctum 02 (Masoterapia)',
      code: 'S-02',
      type: 'Cabina Sensorial de Recuperación Miofascial',
      equipment: ['Camilla ergonómica acolchada', 'Difusor de aromaterapia de cedro', 'Sistema de sonido envolvente 432Hz'],
      status: 'DISPONIBLE'
    },
    {
      id: 'CAB-03',
      name: 'Sala de Podología Clínica',
      code: 'POD-01',
      type: 'Gabinete Clínico Especializado en Pie Masculino',
      equipment: ['Sillón podológico eléctrico', 'Micromotor con aspiración integrada', 'Esterilizador autoclave clase B'],
      status: 'EN SANITIZACIÓN'
    }
  ];

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
    { id: 'LOG-8803', timestamp: '2026-08-21 15:30:19', actor: 'Matías Harb', role: 'ADMINISTRADOR', action: 'Ajuste de comisión especialista Valentina Ross a 35%', ip: '190.161.88.90', severity: 'INFO' },
    { id: 'LOG-8804', timestamp: '2026-08-21 14:10:44', actor: 'Valentina Ross', role: 'ESPECIALISTA', action: 'Registro de evolución clínica paciente PAT-001', ip: '192.168.1.112', severity: 'INFO' },
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
      title: 'Upgrade a Suite VIP Sanctum 01',
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

  saveStaffMember(staff: StaffMember): void {
    this.showToast(`Cambios guardados para ${staff.name}: comisión ${staff.commissionRate}%`);
  }

  toggleStaffStatus(staff: StaffMember): void {
    staff.status = staff.status === 'ACTIVO' ? 'EN PAUSA' : 'ACTIVO';
    this.showToast(`${staff.name} quedó ${staff.status.toLowerCase()}.`);
  }

  toggleProtocol(name: string): void {
    this.protocolStatuses[name] = !this.protocolStatuses[name];
    this.showToast(`${name}: ${this.protocolStatuses[name] ? 'protocolo activado' : 'protocolo desactivado'}.`);
  }

  addProtocolDemo(): void {
    this.showToast('Formulario de nuevo protocolo listo para conectar con el catálogo backend.');
  }

  // -------------------------------------------------------------
  // OPERACIONES DE AGENDA Y RESERVAS
  // -------------------------------------------------------------
  get waitingClients(): BookingAdminItem[] {
    return this.bookings.filter(b => b.status === 'EN ESPERA');
  }

  get activeSessions(): BookingAdminItem[] {
    return this.bookings.filter(b => b.status === 'EN SESIÓN');
  }

  get staffStatuses() {
    return this.staffList.filter(s => s.role === 'ESPECIALISTA').map(staff => {
      const activeSession = this.activeSessions.find(b => b.prof === staff.name);
      if (activeSession) {
        const isEndingSoon = staff.name === 'Valentina Ross'; // Mock for demo
        return {
          ...staff,
          availability: isEndingSoon ? 'PRÓXIMAMENTE LIBRE' : 'OCUPADO',
          detail: isEndingSoon ? `Finalizando en ${activeSession.room}` : `En sesión (${activeSession.room})`
        };
      }

      const waitingClient = this.waitingClients.find(b => b.prof === staff.name);
      if (waitingClient) {
        return {
          ...staff,
          availability: 'OCUPADO',
          detail: `Por iniciar sesión`
        };
      }

      return {
        ...staff,
        availability: 'LIBRE',
        detail: `Disponible`
      };
    });
  }

  get filteredBookings(): BookingAdminItem[] {
    const role = this.currentUser().role;
    let list = this.bookings;

    // Si es Especialista, solo ve sus turnos asignados
    if (role === 'ESPECIALISTA') {
      list = list.filter(b => b.prof === this.currentUser().name);
    }

    // Si es Cliente, solo ve sus citas
    if (role === 'CLIENTE') {
      list = list.filter(b => b.client.toLowerCase().includes('carlos mendoza'));
    }

    // Filtro por sala
    if (this.selectedRoomFilter !== 'TODAS') {
      list = list.filter(b => b.room.includes(this.selectedRoomFilter));
    }

    return list;
  }

  updateBookingStatus(booking: BookingAdminItem, newStatus: BookingStatus): void {
    booking.status = newStatus;
    this.showToast(`Reserva ${booking.id} (${booking.client}) actualizada a: ${newStatus}`);

    // Registrar evento de auditoría
    this.auditLogs.unshift({
      id: `LOG-${Math.floor(1000 + Math.random() * 9000)}`,
      timestamp: this.serverTimeService.getNowISOString().replace('T', ' ').substring(0, 19),
      actor: this.currentUser().name,
      role: this.currentUser().role,
      action: `Cambio de estado de reserva ${booking.id} a ${newStatus}`,
      ip: '192.168.1.100',
      severity: 'INFO'
    });
  }

  deleteBooking(booking: BookingAdminItem): void {
    const role = this.currentUser().role;
    if (role === 'SUPER ADMIN' || role === 'ADMINISTRADOR') {
      if (confirm('¿Confirma que desea borrar permanentemente esta reserva del sistema?')) {
        this.bookings = this.bookings.filter(b => b.id !== booking.id);
        this.showToast(`Reserva ${booking.id} eliminada permanentemente a las ${this.serverTime}.`);
        this.auditLogs.unshift({
          id: `LOG-${Math.floor(1000 + Math.random() * 9000)}`,
          timestamp: this.serverTime,
          actor: this.currentUser().name,
          role: role,
          action: `Eliminación permanente de reserva ${booking.id}`,
          ip: '192.168.1.100',
          severity: 'SECURITY'
        });
      }
    } else if (role === 'CLIENTE') {
      if (confirm('¿Estás seguro que deseas cancelar tu cita? Se aplicará la política de cancelación.')) {
        booking.status = 'CANCELADA';
        this.showToast(`Tu cita ${booking.id} ha sido anulada a las ${this.serverTime}.`);
      }
    } else {
      this.showToast('Acceso denegado: No tienes permisos para borrar citas.');
    }
  }

  saveManualBooking(): void {
    if (!this.newBooking.client || !this.newBooking.service) {
      this.showToast('Por favor completa el nombre del cliente y el servicio.');
      return;
    }

    const created: BookingAdminItem = {
      id: `REC-${Math.floor(100000 + Math.random() * 900000)}`,
      client: this.newBooking.client || 'Cliente Walk-in',
      clientPhone: this.newBooking.clientPhone || '+56 9 ',
      service: this.newBooking.service || 'Descompresión Miofascial',
      serviceCategory: this.newBooking.serviceCategory || 'MASAJE',
      prof: this.newBooking.prof || 'Valentina Ross',
      room: this.newBooking.room || 'Sanctum 01 (Descompresión)',
      date: this.newBooking.date || '2026-08-22',
      time: this.newBooking.time || '15:00',
      price: this.newBooking.price || 65000,
      status: 'CONFIRMADA',
      paymentStatus: this.newBooking.paymentStatus || 'PAGADO',
      paymentMethod: this.newBooking.paymentMethod || 'POS TRANSBANK',
      notes: this.newBooking.notes || 'Agendada manualmente en Front-Desk.'
    };

    this.bookings.unshift(created);
    this.showManualBookingModal = false;
    this.showToast(`Nueva cita agendada con éxito: ${created.id} para ${created.client}`);

    // Si ya pagó, registrar en la caja
    if (created.paymentStatus === 'PAGADO') {
      this.cashTransactions.unshift({
        id: `TRX-${Math.floor(100 + Math.random() * 900)}`,
        time: created.time,
        client: created.client,
        service: created.service,
        amount: created.price,
        method: created.paymentMethod || 'POS TRANSBANK',
        voucherCode: `VOUCH-${Math.floor(10000 + Math.random() * 90000)}`,
        processedBy: this.currentUser().name
      });
    }
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
  // OPERACIONES DE CAJA CHICA & POS
  // -------------------------------------------------------------
  get totalCollectedToday(): number {
    return this.cashTransactions.reduce((acc, curr) => acc + curr.amount, 0);
  }

  get totalTransbank(): number {
    return this.cashTransactions.filter(t => t.method === 'POS TRANSBANK').reduce((a, b) => a + b.amount, 0);
  }

  get totalCash(): number {
    return this.cashTransactions.filter(t => t.method === 'EFECTIVO').reduce((a, b) => a + b.amount, 0);
  }

  get totalStripe(): number {
    return this.cashTransactions.filter(t => t.method === 'STRIPE WEB').reduce((a, b) => a + b.amount, 0);
  }

  registerQuickPayment(): void {
    if (!this.newPayment.client) {
      this.showToast('Ingresa el nombre del cliente para registrar el cobro.');
      return;
    }

    this.cashTransactions.unshift({
      id: `TRX-${Math.floor(100 + Math.random() * 900)}`,
      time: this.serverTimeService.getNowISOString().substring(11, 16),
      client: this.newPayment.client,
      service: this.newPayment.service,
      amount: this.newPayment.amount,
      method: this.newPayment.method,
      voucherCode: `VOUCH-${Math.floor(10000 + Math.random() * 90000)}`,
      processedBy: this.currentUser().name
    });

    this.newPayment.client = '';
    this.showToast(`Cobro registrado en caja correctamente. Total actualizado.`);
  }

  deleteTransaction(trxId: string): void {
    const role = this.currentUser().role;
    if (role === 'SUPER ADMIN' || role === 'ADMINISTRADOR') {
      if (confirm(`¿Está seguro de anular el pago ${trxId}? El dinero será extornado.`)) {
        this.cashTransactions = this.cashTransactions.filter(t => t.id !== trxId);
        this.showToast(`Comprobante ${trxId} anulado y extornado de la caja a las ${this.serverTime}.`);
      }
    } else {
      this.showToast('Acceso denegado: Solo Gerencia o TI pueden anular pagos.');
    }
  }

  closeDailyCashRegister(): void {
    const total = this.totalCollectedToday;
    this.showToast(`Cierre de caja completado. Total arqueado: $${total.toLocaleString('es-CL')} CLP. Comprobante archivado.`);
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
