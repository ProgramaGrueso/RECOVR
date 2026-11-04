import { TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { vi } from 'vitest';
import { HeroComponent } from './hero.component';
import { BookingService } from '../../services/booking.service';
import { CursorService } from '../../services/cursor.service';

/**
 * Bug: el hero registraba un listener de scroll en window (y el de fin de la intro) y nunca lo quitaba. Cada vez que
 * se volvía a la portada quedaba otro listener vivo apuntando a un DOM ya destruido (fuga de memoria y trabajo
 * extra en cada scroll).
 */
describe('HeroComponent', () => {
  beforeEach(async () => {
    window.matchMedia = vi.fn().mockReturnValue({ matches: false, addEventListener: vi.fn(), removeEventListener: vi.fn() });
    TestBed.configureTestingModule({
      imports: [HeroComponent],
      providers: [
        { provide: BookingService, useValue: { navigateToBooking: vi.fn() } },
        { provide: CursorService, useValue: { setCursor: vi.fn(), resetCursor: vi.fn() } }
      ]
    });
    // El orbe 3D (WebGL) no es parte de esta prueba
    TestBed.overrideComponent(HeroComponent, { set: { imports: [CommonModule], schemas: [NO_ERRORS_SCHEMA] } });
    await TestBed.compileComponents();
  });

  afterEach(() => vi.restoreAllMocks());

  it('quita de window todos los listeners que agregó al destruirse', () => {
    const agregados: [string, unknown][] = [];
    const quitados: [string, unknown][] = [];
    vi.spyOn(window, 'addEventListener').mockImplementation((tipo: string, fn: unknown) => { agregados.push([tipo, fn]); });
    vi.spyOn(window, 'removeEventListener').mockImplementation((tipo: string, fn: unknown) => { quitados.push([tipo, fn]); });

    const fixture = TestBed.createComponent(HeroComponent);
    fixture.detectChanges();
    const scroll = agregados.filter(([tipo]) => tipo === 'scroll');
    expect(scroll.length).toBeGreaterThan(0);

    fixture.destroy();
    for (const [tipo, fn] of scroll) {
      expect(quitados).toContainEqual([tipo, fn]);
    }
  });
});
