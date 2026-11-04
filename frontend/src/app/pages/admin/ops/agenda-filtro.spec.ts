import { filtrarAgenda, FiltroAgenda } from './agenda-filtro';
import { ReservaOps } from '../../../models/ops.model';
import { diaRelativo } from './ops.util';

const AHORA = new Date('2026-11-10T15:00:00');
const r = (id: number, fechaHora: string, extra: Partial<ReservaOps> = {}): ReservaOps =>
  ({ id, fechaHora, estadoOperativo: 'CONFIRMADA', salaId: 1, clienteNombre: 'Ana Quispe', servicioNombre: 'RC-01 Tántrico', ...extra } as ReservaOps);
const filtro = (f: Partial<FiltroAgenda>): FiltroAgenda => ({ rango: 'TODAS', estado: '', salaId: '', texto: '', ...f });

describe('filtrarAgenda', () => {
  const reservas = [
    r(1, '2026-11-09T18:00:00'),
    r(2, '2026-11-10T12:00:00'),
    r(3, '2026-11-10T21:00:00', { salaId: 2, estadoOperativo: 'EN_ESPERA' }),
    r(4, '2026-11-11T13:30:00', { clienteNombre: 'Bruno Ñahui' }),
    r(5, '2026-11-20T16:30:00', { servicioNombre: 'RC-06 VIP' })
  ];

  it('separa hoy, mañana, la semana y las pasadas', () => {
    expect(filtrarAgenda(reservas, filtro({ rango: 'HOY' }), AHORA).map(x => x.id)).toEqual([2, 3]);
    expect(filtrarAgenda(reservas, filtro({ rango: 'MANANA' }), AHORA).map(x => x.id)).toEqual([4]);
    expect(filtrarAgenda(reservas, filtro({ rango: 'SEMANA' }), AHORA).map(x => x.id)).toEqual([2, 3, 4]);
    expect(filtrarAgenda(reservas, filtro({ rango: 'PASADAS' }), AHORA).map(x => x.id)).toEqual([1]);
  });

  it('"todas" va de la más reciente a la más antigua', () => {
    expect(filtrarAgenda(reservas, filtro({}), AHORA).map(x => x.id)).toEqual([5, 4, 3, 2, 1]);
  });

  it('filtra por estado, cabina y texto (cliente o servicio, sin distinguir mayúsculas)', () => {
    expect(filtrarAgenda(reservas, filtro({ estado: 'EN_ESPERA' }), AHORA).map(x => x.id)).toEqual([3]);
    expect(filtrarAgenda(reservas, filtro({ salaId: '2' }), AHORA).map(x => x.id)).toEqual([3]);
    expect(filtrarAgenda(reservas, filtro({ texto: 'ñahui' }), AHORA).map(x => x.id)).toEqual([4]);
    expect(filtrarAgenda(reservas, filtro({ texto: 'vip' }), AHORA).map(x => x.id)).toEqual([5]);
  });

  it('diaRelativo suma y resta días respetando el cambio de mes', () => {
    expect(diaRelativo(1, '2026-11-30')).toBe('2026-12-01');
    expect(diaRelativo(-30, '2026-11-04')).toBe('2026-10-05');
  });
});
