import { describe, expect, it } from 'vitest';
import { interpretar, tratamientoCitado } from './asistente-ordenes';
import { ServiceItem } from '../../models/service.model';

const s = (id: string, code: string, name: string, category: string): ServiceItem =>
  ({ id, code, name, category, description: '', durationMinutes: 60, price: 100, imageUrl: '' });

const CARTA = [
  s('rc-01', 'RC-01', 'Masaje Tántrico Sensitivo', 'TÁNTRICO'),
  s('rc-02', 'RC-02', 'Masaje Nuru Body Slide', 'NURU'),
  s('rc-03', 'RC-03', 'Masaje Relajante & Descontracturante', 'DESCONTRACTURANTE'),
  s('rc-05', 'RC-05', 'Masaje a Cuatro Manos', '4 MANOS'),
  s('rc-06', 'RC-06', 'Masaje RECOVR VIP Hidromasaje', 'VIP'),
];

describe('órdenes de la asistente', () => {
  it('lleva a las secciones de la portada', () => {
    expect(interpretar('llévame a las cabinas', CARTA)).toMatchObject({ tipo: 'seccion', id: 'cabinas' });
    expect(interpretar('muéstrame los ositos', CARTA)).toMatchObject({ tipo: 'seccion', id: 'ositos' });
    expect(interpretar('quiero ver la ubicación', CARTA)).toMatchObject({ tipo: 'seccion', id: 'ubicacion' });
  });

  it('reconoce tratamientos por nombre o código para verlos o reservarlos', () => {
    expect(tratamientoCitado('el nuru', CARTA)?.code).toBe('RC-02');
    expect(tratamientoCitado('rc-06 por favor', CARTA)?.code).toBe('RC-06');
    expect(tratamientoCitado('a 4 manos', CARTA)?.code).toBe('RC-05');
    expect(interpretar('quiero reservar el descontracturante', CARTA)).toMatchObject({ tipo: 'reservar', servicio: { code: 'RC-03' } });
    expect(interpretar('muéstrame el vip', CARTA)).toMatchObject({ tipo: 'ver', servicio: { code: 'RC-06' } });
  });

  it('deja a la IA las preguntas de precio, horario o disponibilidad', () => {
    expect(interpretar('¿cuánto cuesta el nuru?', CARTA)).toBeNull();
    expect(interpretar('muéstrame los horarios del sábado', CARTA)).toBeNull();
    expect(interpretar('hola, qué me recomiendas para el estrés', CARTA)).toBeNull();
  });

  it('ofrece WhatsApp e ingresar', () => {
    expect(interpretar('quiero hablar con una persona', CARTA)).toMatchObject({ tipo: 'enlace' });
    expect(interpretar('ingresar a mi cuenta', CARTA)).toMatchObject({ tipo: 'ruta', ruta: '/admin/login' });
  });
});
