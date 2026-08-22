import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent {
  stats = [
    { label: 'INGRESOS MES', value: '$14,850 USD', change: '+12.4%' },
    { label: 'RESERVAS COMPLETADAS', value: '184', change: '+8.1%' },
    { label: 'TASA DE OCUPACIÓN', value: '91.2%', change: '+4.5%' },
    { label: 'CANCELACIONES', value: '2.1%', change: '-0.8%' }
  ];

  bookings = [
    { id: 'REC-908123', client: 'Carlos Mendoza', service: 'Descompresión Miofascial', prof: 'Valentina Ross', date: '2026-08-21 16:00', status: 'CONFIRMADA' },
    { id: 'REC-871234', client: 'Ignacio Silva', service: 'Podología Clínica', prof: 'Elena Roth', date: '2026-08-21 17:30', status: 'COMPLETADA' },
    { id: 'REC-761928', client: 'Esteban Paz', service: 'Masaje Deportivo', prof: 'Gabriel Vane', date: '2026-08-22 09:00', status: 'PENDIENTE' },
    { id: 'REC-654129', client: 'Mariano Torres', service: 'Perfilado Podológico', prof: 'Elena Roth', date: '2026-08-22 10:30', status: 'CONFIRMADA' }
  ];
}
