import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { vi } from 'vitest';
import { BookingFlowComponent } from './booking-flow.component';
import { BookingService } from '../../services/booking.service';
import { CatalogService } from '../../services/catalog.service';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { ServerTimeService } from '../../services/server-time.service';
import { TurnoDisponible } from '../../models/booking.model';

describe('BookingFlowComponent', () => {
  let componente: BookingFlowComponent;
  let respuestas: Record<string, Subject<TurnoDisponible[]>>;

  beforeEach(async () => {
    respuestas = {};
    const booking = {
      selectedService$: of(null),
      clearSelectedService: vi.fn(),
      availability: vi.fn((_c: string, _p: string, fecha: string) => (respuestas[fecha] = new Subject<TurnoDisponible[]>()))
    };
    TestBed.configureTestingModule({
      imports: [BookingFlowComponent],
      providers: [
        { provide: BookingService, useValue: booking },
        { provide: CatalogService, useValue: { getFeaturedServices: () => of([]), getProfessionals: () => of([]) } },
        { provide: AuthService, useValue: { isClient: () => true, user: () => null } },
        { provide: ApiService, useValue: {} },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ServerTimeService, useValue: { getNowISOString: () => '2026-11-10T15:00:00.000' } }
      ]
    });
    TestBed.overrideTemplate(BookingFlowComponent, '');
    await TestBed.compileComponents();
    const fixture = TestBed.createComponent(BookingFlowComponent);
    componente = fixture.componentInstance;
    fixture.detectChanges();
    componente.services = [{ id: 's1', code: 'RC-01' } as never];
    componente.professionals = [{ id: 'p1', name: 'Ely' } as never];
    componente.bookingForm.patchValue({ serviceId: 's1', professionalId: 'p1' });
  });

  /**
   * Bug: al cambiar de fecha rápido, la respuesta de una fecha anterior podía llegar después y pisar los turnos de la
   * fecha elegida (se mostraban horarios de otro día como disponibles).
   */
  it('una respuesta vieja de disponibilidad no pisa los turnos de la fecha elegida', () => {
    componente.currentStep = 3; // consulta 2026-11-10
    componente.bookingForm.patchValue({ date: '2026-11-11' });

    respuestas['2026-11-11'].next([{ hora: '18:00', disponible: true, motivo: null }]);
    respuestas['2026-11-10'].next([{ hora: '12:00', disponible: false, motivo: 'Ely ya tiene una sesión a esa hora' }]);

    expect(componente.slots).toEqual([{ hora: '18:00', disponible: true, motivo: null }]);
  });

  /** Bug: "reservar otra" dejaba la fecha vacía y el paso de turnos no consultaba nada hasta elegirla de nuevo. */
  it('reiniciar el flujo vuelve a la fecha de hoy (Lima), no a una fecha vacía', () => {
    componente.bookingForm.patchValue({ date: '2026-11-20' });
    componente.resetFlow();
    expect(componente.bookingForm.get('date')?.value).toBe('2026-11-10');
  });
});
