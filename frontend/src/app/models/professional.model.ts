export interface Professional {
  id: string;
  name: string;
  title: string;
  specialty: string;
  photoUrl: string;
  videoUrl?: string;
  bio: string;
  duties: string;
  availability: 'Disponible hoy' | 'Turnos esta semana' | 'Consulta previa' | 'Turnos por agenda' | string;
  category?: 'MASAJE' | 'PODOLOGÍA' | 'FACIAL' | 'CORPORAL' | string;
}


