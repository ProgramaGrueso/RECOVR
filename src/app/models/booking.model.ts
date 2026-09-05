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
}
