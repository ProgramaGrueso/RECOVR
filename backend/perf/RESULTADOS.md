# Rendimiento: antes y después (Fase 4)

**Entorno:** 1 máquina con 4 vCPU, MySQL 8 en Docker y datos del perfil `demo` (~1.800 citas, 145 clientes).
Prueba: `backend/perf/prueba-carga.mjs`, con 400 peticiones por escenario (tras una ronda de calentamiento) y un token ADMIN.

## 1. Consultas SQL por operación (`RendimientoConsultasTest`, H2 con el seeder)

| Operación | Antes | Después |
|---|---:|---:|
| Listado de reservas (200) | 305 | **2** |
| Búsqueda por rango y estado (30 días) | 395 | **1** |
| Resumen del día | 456 | **3** |
| Estado en vivo de cabinas | 470 | **2** |

Causa: relaciones `@ManyToOne` EAGER y `Reserva.pago` (OneToOne inverso), con una consulta extra por fila (N+1).
Solución: relaciones LAZY y `@EntityGraph` en las consultas de listados. El test falla si vuelve a aparecer un N+1.

## 2. Carga HTTP, 1 instancia, concurrencia 20

| Escenario | Antes req/s | Antes p50 / p95 (ms) | Después req/s | Después p50 / p95 (ms) |
|---|---:|---:|---:|---:|
| Listado de reservas (agenda, ~1.000 filas) | 7 | 3018 / 3820 | **72** | **269 / 433** |
| Búsqueda por rango y estado (1 mes) | 20 | 990 / 1295 | **192** | **107 / 149** |
| Resumen del día (KPI) | 18 | 1120 / 1553 | **204** | **95 / 146** |
| Estado en vivo de cabinas | 17 | 1144 / 1529 | **198** | **101 / 165** |
| Disponibilidad de turnos | 94 | 202 / 340 | **110** | **176 / 264** |
| Catálogo de servicios (en caché) | 731 | 26 / 45 | 739 | 25 / 48 |

Con la **paginación** que ahora usa el panel (una página de 100 en la ventana de la semana), el listado sube a
**346 req/s, con p50 de 56 ms y p95 de 86 ms**, y cada respuesta pesa 35 KB en lugar de 430 KB (antes de gzip).

## 3. Dos réplicas detrás de nginx (`docker compose --profile app up --scale api=2`), concurrencia 40

| Escenario | req/s | p50 / p95 (ms) | Errores |
|---|---:|---:|---:|
| Listado de reservas (agenda) | 58 | 567 / 1292 | 0 |
| Búsqueda por rango y estado | 167 | 210 / 423 | 0 |
| Resumen del día | 162 | 227 / 432 | 0 |
| Estado en vivo de cabinas | 197 | 187 / 362 | 0 |
| Disponibilidad de turnos | 107 | 317 / 704 | 0 |
| Catálogo de servicios | 635 | 54 / 130 | 0 |

Lectura honesta: en **una sola máquina de 4 vCPU** (compartida entre MySQL, tres JVM, nginx y el generador de carga)
la segunda réplica no suma rendimiento, porque el límite es la CPU. Lo que esta prueba demuestra es que el backend
es **stateless**: un token emitido por una réplica vale en la otra, nginx reparte en round-robin (cabecera
`X-Upstream`) y no hubo errores. La ganancia de escalar en horizontal aparece al poner las réplicas en máquinas distintas.

Para repetirla:
```bash
API=http://localhost:8095/api CORREO=admin.demo@example.com CLAVE='<clave demo>' node backend/perf/prueba-carga.mjs
# variables opcionales: CONCURRENCIA, PETICIONES, LISTADO_QUERY="?desde=2026-11-01&hasta=2026-11-08&tamano=100"
```
