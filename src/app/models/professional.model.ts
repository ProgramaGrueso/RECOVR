export interface Professional {
  id: string;
  name: string;
  title: string;
  specialty: string;
  photoUrl: string;
  bio: string;
  duties: string;
  availability: 'Disponible hoy' | 'Turnos esta semana' | 'Consulta previa';
  category?: 'MASAJE' | 'PODOLOGÍA';
}

