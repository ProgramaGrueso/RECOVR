#!/usr/bin/env node
/**
 * Prueba de carga sencilla, sin dependencias (Node 18+). Inicia sesión como recepción/admin demo y lanza peticiones
 * concurrentes contra los endpoints más usados del panel. Reporta peticiones por segundo y latencias p50/p95/p99.
 *
 * Uso:
 *   API=http://localhost:8095/api CORREO=admin.demo@example.com CLAVE='...' node backend/perf/prueba-carga.mjs
 * Variables opcionales: CONCURRENCIA (20), PETICIONES (400 por escenario).
 */
const API = process.env.API ?? 'http://localhost:8095/api';
const CONCURRENCIA = Number(process.env.CONCURRENCIA ?? 20);
const PETICIONES = Number(process.env.PETICIONES ?? 400);
const correo = process.env.CORREO;
const clave = process.env.CLAVE;
if (!correo || !clave) {
  console.error('Define CORREO y CLAVE de una cuenta ADMIN o RECEPCIONISTA (perfil demo).');
  process.exit(1);
}

const login = await fetch(`${API}/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ correo, password: clave })
});
if (!login.ok) {
  console.error('No se pudo iniciar sesión:', login.status, await login.text());
  process.exit(1);
}
const { token } = await login.json();
const headers = { Authorization: `Bearer ${token}` };

const hoy = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date());
const masDias = (n) => { const d = new Date(`${hoy}T12:00:00`); d.setDate(d.getDate() + n); return d.toISOString().slice(0, 10); };

const escenarios = [
  ['Listado de reservas (agenda del panel)', `/db/reservas${process.env.LISTADO_QUERY ?? ''}`],
  ['Búsqueda por rango y estado (1 mes)', `/db/reservas/buscar?inicio=${hoy}T00:00:00&fin=${masDias(30)}T00:00:00&estado=CONFIRMADA`],
  ['Resumen del día (KPI)', '/operacion/resumen'],
  ['Estado en vivo de cabinas', '/operacion/salas'],
  ['Disponibilidad de turnos', `/db/reservas/disponibilidad?empleadoId=1&servicioId=1&fecha=${masDias(7)}`],
  ['Catálogo de servicios', '/servicios'],
];

function percentil(ordenadas, p) {
  return ordenadas[Math.min(ordenadas.length - 1, Math.floor((p / 100) * ordenadas.length))];
}

async function escenario(nombre, ruta) {
  const tiempos = [];
  let errores = 0;
  let bytes = 0;
  let siguiente = 0;
  const inicio = performance.now();
  async function trabajador() {
    while (siguiente < PETICIONES) {
      siguiente++;
      const t0 = performance.now();
      try {
        const r = await fetch(API + ruta, { headers });
        const cuerpo = await r.arrayBuffer();
        bytes += cuerpo.byteLength;
        if (!r.ok) errores++;
      } catch {
        errores++;
      }
      tiempos.push(performance.now() - t0);
    }
  }
  await Promise.all(Array.from({ length: CONCURRENCIA }, trabajador));
  const total = (performance.now() - inicio) / 1000;
  tiempos.sort((a, b) => a - b);
  return {
    escenario: nombre,
    'req/s': Math.round(tiempos.length / total),
    'p50 ms': Math.round(percentil(tiempos, 50)),
    'p95 ms': Math.round(percentil(tiempos, 95)),
    'p99 ms': Math.round(percentil(tiempos, 99)),
    'KB/resp': Math.round(bytes / tiempos.length / 1024),
    errores,
  };
}

console.log(`API ${API} · concurrencia ${CONCURRENCIA} · ${PETICIONES} peticiones por escenario`);
const resultados = [];
for (const [nombre, ruta] of escenarios) {
  await escenario(nombre, ruta); // calentamiento (JIT y caches)
  resultados.push(await escenario(nombre, ruta));
}
console.table(resultados);
