export type AdminRole = 'SUPER ADMIN' | 'ADMINISTRADOR' | 'RECEPCIONISTA' | 'ESPECIALISTA' | 'CLIENTE';

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: AdminRole;
  roleTitle: string;
  roleScope: string;
  avatar: string;
  badgeTone: 'gold' | 'burgundy' | 'silver' | 'teal' | 'light';
  phone?: string;
  points?: number;
  cancellationsThisMonth?: number;
  bonos?: { remaining: number; total: number; name: string };
  notes?: string;
}

export interface PointTransaction {
  id: string;
  date: string;
  amount: number;
  reason: string;
  type: 'EARNED' | 'REDEEMED';
}

export interface BenefitItem {
  id: string;
  title: string;
  pointsCost: number;
  description: string;
  icon: string;
  category: string;
}

export interface ClientBookingItem {
  id: string;
  service: string;
  prof: string;
  room: string;
  date: string;
  time: string;
  price: number;
  status: BookingStatus;
  notes?: string;
}

export type BookingStatus = 'CONFIRMADA' | 'EN ESPERA' | 'EN SESIÓN' | 'COMPLETADA' | 'CANCELADA';

export interface BookingAdminItem {
  id: string;
  client: string;
  clientPhone: string;
  service: string;
  serviceCategory: 'MASAJE' | 'PODOLOGÍA' | 'RITUAL';
  prof: string;
  room: string;
  date: string;
  time: string;
  price: number;
  status: BookingStatus;
  paymentStatus: 'PAGADO' | 'PENDIENTE' | 'ABONO';
  paymentMethod?: 'EFECTIVO' | 'POS TRANSBANK' | 'STRIPE WEB';
  notes?: string;
}

export interface ClinicalEvolutionNote {
  id: string;
  date: string;
  specialistName: string;
  treatment: string;
  findings: string;
  painScale: number; // 1 to 10
  recommendations: string;
}

export interface ClinicalRecord {
  patientId: string;
  patientName: string;
  age: number;
  emergencyContact: string;
  anamnesis: string;
  medicalConditions: string[];
  allergies: string[];
  preferredPressure: 'SUAVE' | 'MEDIA' | 'ALTA' | 'DESCOMPRESIÓN MÁXIMA';
  oilPreference: string;
  restrictedAreas: string[];
  notesHistory: ClinicalEvolutionNote[];
}

export interface CashTransaction {
  id: string;
  time: string;
  client: string;
  service: string;
  amount: number;
  method: 'EFECTIVO' | 'POS TRANSBANK' | 'STRIPE WEB';
  voucherCode: string;
  processedBy: string;
}

export interface StaffMember {
  id: string;
  name: string;
  role: AdminRole;
  specialty: string;
  email: string;
  phone: string;
  shift: string;
  commissionRate: number; // Percentage
  monthlySessions: number;
  status: 'ACTIVO' | 'EN PAUSA' | 'LICENCIA';
}

export interface CabinRoom {
  id: string;
  name: string;
  code: string;
  type: string;
  equipment: string[];
  status: 'DISPONIBLE' | 'OCUPADA' | 'EN SANITIZACIÓN';
  currentTherapist?: string;
}

export interface SystemAuditLog {
  id: string;
  timestamp: string;
  actor: string;
  role: AdminRole;
  action: string;
  ip: string;
  severity: 'INFO' | 'WARN' | 'SECURITY';
}
