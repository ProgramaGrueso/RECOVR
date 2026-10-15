/** Modelos de la zona administrativa: reflejan 1:1 las respuestas de la API Spring Boot. */

export type EstadoOperativo = 'EN_ESPERA' | 'CONFIRMADA' | 'EN_SESION' | 'COMPLETADA' | 'CANCELADA';

export interface ReservaOps {
  id: number;
  clienteId: number; clienteNombre: string;
  empleadoId: number; empleadoNombre: string;
  servicioId: number; servicioNombre: string;
  salaId: number; salaNombre: string;
  fechaHora: string; finBloque: string;
  estado: 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'COMPLETADA';
  estadoOperativo: EstadoOperativo;
  checkIn: string | null;
  observacion: string | null;
  precio: number;
  pagado: boolean;
  metodoPago: string | null;
}

export interface ReservaForm {
  clienteId: number | null;
  empleadoId: number | null;
  servicioId: number | null;
  salaId: number | null;
  fecha: string;
  hora: string;
}

export interface ClienteOps { id: number; nombre: string; correo: string; telefono: string | null; usuarioId: number | null; }
export interface EmpleadoOps { id: number; nombre: string; especialidad: string | null; telefono: string | null; }
export interface EmpleadoEstado extends EmpleadoOps {
  activo: boolean;
  estado: 'LIBRE' | 'EN_SESION' | 'POR_INICIAR';
  sala: string | null; hasta: string | null; proximaReserva: string | null;
}
export interface SalaOps { id: number; nombre: string; capacidad: number | null; }
export interface SalaEstado extends SalaOps {
  estado: 'DISPONIBLE' | 'OCUPADA' | 'ESPERANDO_CLIENTE' | 'EN_SANITIZACION';
  terapeuta: string | null; hasta: string | null; proximaReserva: string | null;
}
export interface ServicioOps {
  id: number; nombre: string; duracionMinutos: number; tiempoLimpiezaMinutos: number; precio: number;
}
export interface ResumenOps {
  ingresosMes: number; ocupacionHoyPct: number; sesionesCompletadasMes: number; agendadasHoy: number;
  pendientesPorConfirmar: number; canceladasMes: number; totalMes: number; tasaCancelacionPct: number;
}
