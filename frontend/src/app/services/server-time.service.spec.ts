import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ServerTimeService } from './server-time.service';
import { fechaInput } from '../pages/admin/ops/ops.util';

/**
 * Bug: la fecha y hora "del negocio" salían de la zona horaria del navegador. Con el navegador en otro país
 * (aquí Madrid) el panel mostraba otra hora y la reserva arrancaba en otro día. El negocio opera en hora de Lima.
 */
declare const process: { env: Record<string, string | undefined> };

describe('Hora del negocio (America/Lima)', () => {
  const zonaOriginal = process.env['TZ'];

  beforeEach(() => {
    process.env['TZ'] = 'Europe/Madrid';
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-11-04T03:30:00Z')); // 22:30 del 3/11 en Lima, 04:30 del 4/11 en Madrid
  });

  afterEach(() => {
    vi.useRealTimers();
    process.env['TZ'] = zonaOriginal;
  });

  it('ServerTimeService da la fecha y hora de Lima sin importar la zona del navegador', () => {
    const servicio = TestBed.inject(ServerTimeService);
    expect(servicio.getNowISOString()).toBe('2026-11-03T22:30:00.000');
  });

  it('el "hoy" por defecto de los formularios es el día de Lima', () => {
    expect(fechaInput()).toBe('2026-11-03');
  });
});
