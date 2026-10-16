export interface BookingRequest {
  serviceId: string;
  professionalId: string;
  date: string;
  timeSlot: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  notes?: string;
}

export interface BookingResponse {
  id: string;
  status: 'PENDIENTE' | 'CONFIRMADA' | 'COMPLETADA' | 'CANCELADA';
  createdAt: string;
  serviceName: string;
  professionalName: string;
  date: string;
  timeSlot: string;
  /** ID numérico de la reserva en el backend (para pagarla). */
  reservaId?: number;
  /** Precio exacto que cobra el servidor: el pago debe ser por este importe completo. */
  price?: number;
}

/** Un turno del día y si se puede reservar con la terapeuta elegida. */
export interface TurnoDisponible {
  hora: string;
  disponible: boolean;
  motivo: string | null;
}
