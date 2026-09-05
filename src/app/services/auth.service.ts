import { inject, Injectable, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AdminRole, AdminUser, ClientBookingItem } from '../models/rbac.model';

export type { AdminRole, AdminUser, ClientBookingItem };

export const DEMO_USERS: Record<AdminRole, AdminUser & { password: string }> = {
  'SUPER ADMIN': {
    id: 'devops',
    name: 'Alexei Rivera',
    email: 'devops@recovr.cl',
    password: 'recovr2026',
    role: 'SUPER ADMIN',
    roleTitle: 'DevOps & Seguridad TI',
    roleScope: 'Infraestructura, APIs y Auditoría Global',
    avatar: 'AR',
    badgeTone: 'gold'
  },
  'ADMINISTRADOR': {
    id: 'admin',
    name: 'Matías Harb',
    email: 'gerencia@recovr.cl',
    password: 'recovr2026',
    role: 'ADMINISTRADOR',
    roleTitle: 'Gerente / Dueño del Spa',
    roleScope: 'Finanzas, Catálogo, Personal y Control Estratégico',
    avatar: 'MH',
    badgeTone: 'burgundy'
  },
  'RECEPCIONISTA': {
    id: 'recepcion',
    name: 'Camila Morales',
    email: 'recepcion@recovr.cl',
    password: 'recovr2026',
    role: 'RECEPCIONISTA',
    roleTitle: 'Front-Desk / Atención al Cliente',
    roleScope: 'Agenda en Tiempo Real, Check-in y Caja Chica',
    avatar: 'CM',
    badgeTone: 'silver'
  },
  'ESPECIALISTA': {
    id: 'especialista',
    name: 'Valentina Ross',
    email: 'valentina@recovr.cl',
    password: 'recovr2026',
    role: 'ESPECIALISTA',
    roleTitle: 'Kinesióloga & Masoterapeuta',
    roleScope: 'Ficha Clínica, Evolución de Paciente y Turnos Propios',
    avatar: 'VR',
    badgeTone: 'teal'
  },
  'CLIENTE': {
    id: 'cliente',
    name: 'Carlos Mendoza',
    email: 'carlos.mendoza@gmail.com',
    password: 'recovr2026',
    role: 'CLIENTE',
    roleTitle: 'Cliente / Paciente Sanctum',
    roleScope: 'Portal Mi Cuenta, Próximas Citas y Bonos de Sesión',
    avatar: 'CM',
    badgeTone: 'light',
    phone: '+56 9 8765 4321',
    points: 350,
    cancellationsThisMonth: 0
  }
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'recovr-admin-session';
  private readonly clientsStorageKey = 'recovr-registered-clients';
  private readonly pointsStorageKey = 'recovr-points-history';
  private readonly bookingsStorageKey = 'recovr-client-bookings';

  // Signal reactivo para el usuario activo
  currentUser = signal<AdminUser | null>(this.loadInitialUser());

  private loadInitialUser(): AdminUser | null {
    const saved = localStorage.getItem(this.storageKey) ?? sessionStorage.getItem(this.storageKey);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed?.role && DEMO_USERS[parsed.role as AdminRole]) {
          const base = DEMO_USERS[parsed.role as AdminRole];
          const dynamicPoints = parsed.role === 'CLIENTE' ? this.getClientPointsFromStorage(parsed.id) : undefined;
          return {
            ...base,
            ...parsed,
            ...(dynamicPoints !== undefined ? { points: dynamicPoints } : {})
          };
        }
        // Verificar si es un cliente registrado dinámicamente
        const registered = this.getRegisteredClients().find(c => c.id === parsed?.id || c.email === parsed?.email);
        if (registered) {
          const dynamicPoints = this.getClientPointsFromStorage(registered.id);
          return {
            ...registered,
            points: dynamicPoints
          };
        }
      } catch {
        // Formato anterior o inválido
      }
    }
    return null;
  }

  get demoUsersList(): Array<AdminUser & { password: string }> {
    return Object.values(DEMO_USERS);
  }

  getRegisteredClients(): Array<AdminUser & { password: string }> {
    try {
      const data = localStorage.getItem(this.clientsStorageKey);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }

  login(userId: string, password: string): boolean {
    const trimmedId = userId.trim().toLowerCase();

    // 1. Buscar en los usuarios demo por email o id
    const foundEntry = Object.values(DEMO_USERS).find(
      u => (u.email.toLowerCase() === trimmedId || u.id.toLowerCase() === trimmedId) && u.password === password
    );

    // Compatibilidad retroactiva con admin@recovr.cl
    if (!foundEntry && trimmedId === 'admin@recovr.cl' && password === 'recovr2026') {
      const user = DEMO_USERS['SUPER ADMIN'];
      this.persistUser(user);
      return true;
    }

    if (foundEntry) {
      if (foundEntry.role === 'CLIENTE') {
        const pts = this.getClientPointsFromStorage(foundEntry.id);
        const cancellations = this.getClientCancellationsFromStorage(foundEntry.id);
        const userToSave = { ...foundEntry, points: pts, cancellationsThisMonth: cancellations };
        this.persistUser(userToSave);
      } else {
        this.persistUser(foundEntry);
      }
      return true;
    }

    // 2. Buscar en clientes registrados dinámicamente
    const regClient = this.getRegisteredClients().find(
      c => (c.email.toLowerCase() === trimmedId || c.id.toLowerCase() === trimmedId) && c.password === password
    );

    if (regClient) {
      const pts = this.getClientPointsFromStorage(regClient.id);
      const cancellations = this.getClientCancellationsFromStorage(regClient.id);
      const userToSave = { ...regClient, points: pts, cancellationsThisMonth: cancellations };
      this.persistUser(userToSave);
      return true;
    }

    return false;
  }

  registerClient(data: { name: string; email: string; phone: string; password: string }): { success: boolean; message: string; user?: AdminUser } {
    const trimmedEmail = data.email.trim().toLowerCase();
    const trimmedPhone = data.phone.trim();
    const trimmedName = data.name.trim();

    if (!trimmedName || trimmedName.length < 3) {
      return { success: false, message: 'Ingresa un nombre válido (mínimo 3 caracteres).' };
    }
    if (!trimmedEmail || !trimmedEmail.includes('@')) {
      return { success: false, message: 'Ingresa un correo electrónico válido.' };
    }
    if (!data.password || data.password.length < 6) {
      return { success: false, message: 'La contraseña debe tener al menos 6 caracteres.' };
    }

    // Comprobar si ya existe
    const existsDemo = Object.values(DEMO_USERS).some(u => u.email.toLowerCase() === trimmedEmail);
    const registered = this.getRegisteredClients();
    const existsRegistered = registered.some(c => c.email.toLowerCase() === trimmedEmail);

    if (existsDemo || existsRegistered) {
      return { success: false, message: 'Ya existe una cuenta con este correo electrónico. Por favor inicia sesión.' };
    }

    // Generar iniciales para avatar
    const nameParts = trimmedName.split(' ');
    const avatar = (nameParts[0][0] + (nameParts[1] ? nameParts[1][0] : nameParts[0][1] || 'C')).toUpperCase();

    const newClient: AdminUser & { password: string } = {
      id: 'cli-' + Math.floor(1000 + Math.random() * 9000),
      name: trimmedName,
      email: trimmedEmail,
      phone: trimmedPhone,
      password: data.password,
      role: 'CLIENTE',
      roleTitle: 'Cliente / Paciente Sanctum',
      roleScope: 'Portal Mi Cuenta, Próximas Citas y Puntos Sanctum',
      avatar: avatar,
      badgeTone: 'light',
      points: 50, // Bono de bienvenida
      cancellationsThisMonth: 0
    };

    registered.push(newClient);
    localStorage.setItem(this.clientsStorageKey, JSON.stringify(registered));

    // Inicializar puntos del cliente con el bono de bienvenida
    this.saveClientPoints(newClient.id, 50);
    this.addPointTransaction({
      id: 'PT-' + Date.now(),
      date: new Date().toISOString().substring(0, 10),
      amount: 50,
      reason: 'Bono de Bienvenida por creación de cuenta',
      type: 'EARNED'
    });

    this.persistUser(newClient);
    return { success: true, message: '¡Cuenta creada con éxito! Se han sumado +50 Puntos de Bienvenida a tu perfil.', user: newClient };
  }

  loginWithRole(role: AdminRole, userId: string, password: string, rememberMe: boolean): boolean {
    const trimmedId = userId.trim().toLowerCase();

    if (role === 'CLIENTE') {
      // Intentar login genérico
      return this.login(userId, password);
    }

    const user = DEMO_USERS[role];
    if (!user || user.id !== userId.trim() || user.password !== password) return false;

    this.persistUser(user, rememberMe);
    return true;
  }

  loginAsRole(role: AdminRole): void {
    const user = DEMO_USERS[role];
    if (user) {
      if (role === 'CLIENTE') {
        const pts = this.getClientPointsFromStorage(user.id);
        this.persistUser({ ...user, points: pts });
      } else {
        this.persistUser(user);
      }
    }
  }

  switchRole(role: AdminRole): void {
    // Si el usuario actual es CLIENTE, impedir cambiar de rol a roles de staff
    if (this.currentUser()?.role === 'CLIENTE' && role !== 'CLIENTE') {
      console.warn('Acceso denegado: El cliente no puede cambiar su sesión a roles del staff.');
      return;
    }
    this.loginAsRole(role);
  }

  private persistUser(user: AdminUser, rememberMe = true): void {
    const storage = rememberMe ? localStorage : sessionStorage;
    storage.setItem(this.storageKey, JSON.stringify(user));
    this.currentUser.set(user);
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    sessionStorage.removeItem(this.storageKey);
    // Asignar null o usuario demo por defecto para que la SPA responda
    this.currentUser.set(null);
  }

  isAuthenticated(): boolean {
    return !!(localStorage.getItem(this.storageKey) ?? sessionStorage.getItem(this.storageKey));
  }

  isClient(): boolean {
    return this.currentUser()?.role === 'CLIENTE';
  }

  // -------------------------------------------------------------
  // GESTIÓN DE PUNTOS RECOVR SANCTUM
  // -------------------------------------------------------------
  private getClientPointsFromStorage(clientId: string): number {
    const key = `recovr_pts_${clientId}`;
    const saved = localStorage.getItem(key);
    if (saved !== null) {
      return parseInt(saved, 10) || 0;
    }
    // Saldo inicial predeterminado para cliente demo
    if (clientId === 'cliente') {
      localStorage.setItem(key, '350');
      return 350;
    }
    return 50;
  }

  private saveClientPoints(clientId: string, points: number): void {
    const key = `recovr_pts_${clientId}`;
    localStorage.setItem(key, points.toString());
  }

  getClientPoints(): number {
    const current = this.currentUser();
    if (!current || current.role !== 'CLIENTE') return 0;
    return this.getClientPointsFromStorage(current.id);
  }

  addClientPoints(points: number, reason: string): number {
    const current = this.currentUser();
    if (!current || current.role !== 'CLIENTE') return 0;

    const currentPts = this.getClientPoints();
    const newTotal = currentPts + points;
    this.saveClientPoints(current.id, newTotal);

    // Registrar en historial
    this.addPointTransaction({
      id: 'PT-' + Date.now(),
      date: new Date().toISOString().substring(0, 10),
      amount: points,
      reason: reason,
      type: 'EARNED'
    });

    // Actualizar el signal reactivo
    this.currentUser.update(u => u ? { ...u, points: newTotal } : null);
    return newTotal;
  }

  redeemClientBenefit(benefitTitle: string, pointsCost: number): { success: boolean; message: string } {
    const current = this.currentUser();
    if (!current || current.role !== 'CLIENTE') return { success: false, message: 'Función exclusiva de clientes.' };

    const currentPts = this.getClientPoints();
    if (currentPts < pointsCost) {
      return {
        success: false,
        message: `Puntos insuficientes. Tienes ${currentPts} pts y necesitas ${pointsCost} pts.`
      };
    }

    const newTotal = currentPts - pointsCost;
    this.saveClientPoints(current.id, newTotal);

    this.addPointTransaction({
      id: 'PT-' + Date.now(),
      date: new Date().toISOString().substring(0, 10),
      amount: pointsCost,
      reason: `Canje de beneficio: ${benefitTitle}`,
      type: 'REDEEMED'
    });

    this.currentUser.set({ ...current, points: newTotal });
    return { success: true, message: `¡Beneficio canjeado exitosamente! Disfruta de "${benefitTitle}". Saldo restante: ${newTotal} pts.` };
  }

  getPointsHistory(): Array<{ id: string; date: string; amount: number; reason: string; type: 'EARNED' | 'REDEEMED' }> {
    try {
      const data = localStorage.getItem(this.pointsStorageKey);
      if (data) return JSON.parse(data);
    } catch {}

    // Eventos iniciales demo para Carlos Mendoza
    const initialEvents = [
      { id: 'PT-01', date: '2026-08-01', amount: 50, reason: 'Bono de Bienvenida Sanctum Club', type: 'EARNED' as const },
      { id: 'PT-02', date: '2026-08-10', amount: 100, reason: 'Sesión completada — Descompresión Miofascial', type: 'EARNED' as const },
      { id: 'PT-03', date: '2026-08-15', amount: 200, reason: 'Check-in y asistencia puntual consecutiva', type: 'EARNED' as const }
    ];
    localStorage.setItem(this.pointsStorageKey, JSON.stringify(initialEvents));
    return initialEvents;
  }

  private addPointTransaction(tx: { id: string; date: string; amount: number; reason: string; type: 'EARNED' | 'REDEEMED' }): void {
    const history = this.getPointsHistory();
    history.unshift(tx);
    localStorage.setItem(this.pointsStorageKey, JSON.stringify(history));
  }

  // -------------------------------------------------------------
  // GESTIÓN DE CANCELACIONES Y POLÍTICA DE INDEMNIZACIÓN
  // -------------------------------------------------------------
  private getClientCancellationsFromStorage(clientId: string): number {
    const key = `recovr_canc_${clientId}`;
    const saved = localStorage.getItem(key);
    return saved !== null ? parseInt(saved, 10) : 0;
  }

  private saveClientCancellations(clientId: string, count: number): void {
    const key = `recovr_canc_${clientId}`;
    localStorage.setItem(key, count.toString());
  }

  getClientCancellations(): number {
    const current = this.currentUser();
    if (!current) return 0;
    return this.getClientCancellationsFromStorage(current.id);
  }

  incrementClientCancellations(): number {
    const current = this.currentUser();
    if (!current) return 0;
    const count = this.getClientCancellations() + 1;
    this.saveClientCancellations(current.id, count);
    this.currentUser.update(u => u ? { ...u, cancellationsThisMonth: count } : null);
    return count;
  }

  // -------------------------------------------------------------
  // GESTIÓN DE CITAS DEL CLIENTE
  // -------------------------------------------------------------
  getClientBookings(): ClientBookingItem[] {
    const current = this.currentUser();
    if (!current) return [];
    const key = `recovr_bookings_${current.id}`;
    try {
      const data = localStorage.getItem(key);
      if (data) return JSON.parse(data);
    } catch {}

    // Cita por defecto para Carlos Mendoza / demo
    const todayStr = new Date().toISOString().substring(0, 10);
    const initialBookings: ClientBookingItem[] = [
      {
        id: 'REC-908123',
        service: 'Descompresión Miofascial',
        prof: 'Valentina Ross',
        room: 'Sanctum 01 (Descompresión)',
        date: todayStr,
        time: '16:00',
        price: 65000,
        status: 'CONFIRMADA'
      }
    ];
    localStorage.setItem(key, JSON.stringify(initialBookings));
    return initialBookings;
  }

  addClientBooking(booking: ClientBookingItem): void {
    const current = this.currentUser();
    if (!current) return;
    const key = `recovr_bookings_${current.id}`;
    const list = this.getClientBookings();
    list.unshift(booking);
    localStorage.setItem(key, JSON.stringify(list));
  }

  canCancelWithoutIndemnity(booking: ClientBookingItem): { canCancelFree: boolean; hoursRemaining: number; deadlineTime: string } {
    try {
      const bookingDateTime = new Date(`${booking.date}T${booking.time}:00`);
      const now = new Date();
      const diffMs = bookingDateTime.getTime() - now.getTime();
      const diffHours = diffMs / (1000 * 60 * 60);

      const deadline = new Date(bookingDateTime.getTime() - 4 * 60 * 60 * 1000);
      const deadlineStr = `${deadline.getHours().toString().padStart(2, '0')}:${deadline.getMinutes().toString().padStart(2, '0')} hrs`;

      // Si la cita es hoy y faltan más de 4 horas
      const canCancelFree = diffHours >= 4;
      return {
        canCancelFree,
        hoursRemaining: Math.max(0, parseFloat(diffHours.toFixed(1))),
        deadlineTime: deadlineStr
      };
    } catch {
      return { canCancelFree: true, hoursRemaining: 5, deadlineTime: '4 horas antes' };
    }
  }

  cancelClientBooking(bookingId: string, acceptFee = false): { success: boolean; message: string; requiresIndemnity: boolean; feeAmount: number } {
    const cancellations = this.getClientCancellations();
    if (cancellations >= 2) {
      return {
        success: false,
        message: 'Has alcanzado el límite de 2 cancelaciones permitidas en el mes. Para casos de fuerza mayor, contacta a Recepción (+56 9 8765 4321).',
        requiresIndemnity: false,
        feeAmount: 0
      };
    }

    const bookings = this.getClientBookings();
    const booking = bookings.find(b => b.id === bookingId);
    if (!booking) {
      return { success: false, message: 'Reserva no encontrada.', requiresIndemnity: false, feeAmount: 0 };
    }

    if (booking.status === 'CANCELADA') {
      return { success: false, message: 'Esta reserva ya fue cancelada previamente.', requiresIndemnity: false, feeAmount: 0 };
    }

    const { canCancelFree } = this.canCancelWithoutIndemnity(booking);
    const fee = Math.round(booking.price * 0.5);

    if (!canCancelFree && !acceptFee) {
      return {
        success: false,
        message: `Atención: Quedan menos de 4 horas para tu turno. Cancelar ahora requiere el cobro de indemnización del 50% ($${fee.toLocaleString('es-CL')} CLP).`,
        requiresIndemnity: true,
        feeAmount: fee
      };
    }

    booking.status = 'CANCELADA';
    const current = this.currentUser();
    if (!current) return { success: false, message: 'Error de sesión.', requiresIndemnity: false, feeAmount: 0 };
    const key = `recovr_bookings_${current.id}`;
    localStorage.setItem(key, JSON.stringify(bookings));
    this.incrementClientCancellations();

    if (!canCancelFree) {
      return {
        success: true,
        message: `Cita cancelada. Se ha aplicado el cargo de indemnización por cancelación tardía ($${fee.toLocaleString('es-CL')} CLP).`,
        requiresIndemnity: true,
        feeAmount: fee
      };
    }

    return {
      success: true,
      message: 'Cita cancelada exitosamente dentro del plazo seguro. No se aplica cargo de indemnización.',
      requiresIndemnity: false,
      feeAmount: 0
    };
  }


  /**
   * Valida si el rol actual tiene acceso a una vista/módulo administrativo específico
   * según la matriz oficial en roles_del_sistema_rbac.md
   */
  canAccess(viewKey: string, targetRole?: AdminRole): boolean {
    const role = targetRole || this.currentUser()?.role;
    if (!role) return false;

    switch (viewKey) {
      case 'RESUMEN':
        return true; // Todos tienen su versión del resumen adaptada a su rol

      case 'AGENDA':
        // Super Admin, Admin, Recepcionista y Especialista (solo sus turnos)
        return ['SUPER ADMIN', 'ADMINISTRADOR', 'RECEPCIONISTA', 'ESPECIALISTA'].includes(role);

      case 'CLINICA':
        // Exclusivo para Especialista por confidencialidad clínica (Super Admin y Admin denegado por privacidad)
        return role === 'ESPECIALISTA';

      case 'CAJA':
        // Recepcionista (operación) y Administrador/Super Admin (supervisión)
        return ['SUPER ADMIN', 'ADMINISTRADOR', 'RECEPCIONISTA'].includes(role);

      case 'PERSONAL':
        // Administrador y Super Admin
        return ['SUPER ADMIN', 'ADMINISTRADOR'].includes(role);

      case 'CATALOGO':
        // Administrador y Super Admin
        return ['SUPER ADMIN', 'ADMINISTRADOR'].includes(role);

      case 'DEVOPS':
        // Exclusivo Super Admin / DevOps
        return role === 'SUPER ADMIN';

      case 'MATRIZ_RBAC':
        // Super Admin y Administrador
        return ['SUPER ADMIN', 'ADMINISTRADOR'].includes(role);

      case 'MI_CUENTA':
        // Cliente
        return role === 'CLIENTE';

      default:
        return false;
    }
  }
}

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/admin/login']);
};
