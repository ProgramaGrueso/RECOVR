/** Modelos de la zona administrativa: reflejan 1:1 las respuestas de la API Spring Boot. */

export type EstadoOperativo = 'EN_ESPERA' | 'CONFIRMADA' | 'EN_SESION' | 'COMPLETADA' | 'CANCELADA';

export interface ReservaOps {
  id: number;
  clienteId: number; clienteNombre: string;
  empleadoId: number; empleadoNombre: string;
  servicioId: number; servicioNombre: string;
  salaId: number; salaNombre: string;
  fechaHora: string; finBloque: string;
  estado: 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'COMPLETADA';
  estadoOperativo: EstadoOperativo;
  checkIn: string | null;
  observacion: string | null;
  precio: number;
  pagado: boolean;
  metodoPago: string | null;
}

export interface ReservaForm {
  clienteId: number | null;
  empleadoId: number | null;
  servicioId: number | null;
  salaId: number | null;
  fecha: string;
  hora: string;
}

export interface ClienteOps { id: number; nombre: string; correo: string; telefono: string | null; usuarioId: number | null; }
export interface EmpleadoOps { id: number; nombre: string; especialidad: string | null; telefono: string | null; bio?: string | null; }
export interface EmpleadoEstado extends EmpleadoOps {
  activo: boolean;
  bio: string | null;
  tieneFoto: boolean;
  estado: 'LIBRE' | 'EN_SESION' | 'POR_INICIAR';
  sala: string | null; hasta: string | null; proximaReserva: string | null;
}
export interface SalaOps { id: number; nombre: string; capacidad: number | null; activa?: boolean; }
/** Periodo en que una terapeuta no atiende (vacaciones, licencia). */
export interface AusenciaOps { id: number; empleadoId: number; desde: string; hasta: string; motivo: string | null; }
export interface SalaEstado extends SalaOps {
  estado: 'DISPONIBLE' | 'OCUPADA' | 'ESPERANDO_CLIENTE' | 'EN_SANITIZACION' | 'MANTENIMIENTO';
  terapeuta: string | null; hasta: string | null; proximaReserva: string | null;
}
export interface ServicioOps {
  id: number; nombre: string; duracionMinutos: number; tiempoLimpiezaMinutos: number; precio: number;
}
export interface ResumenOps {
  ingresosMes: number; ocupacionHoyPct: number; sesionesCompletadasMes: number; agendadasHoy: number;
  pendientesPorConfirmar: number; canceladasMes: number; totalMes: number; tasaCancelacionPct: number;
}

// ---- Portal del cliente ----
export interface MiCuentaOps {
  nombre: string; correo: string; telefono: string | null;
  puntos: number; nivel: string; siguienteNivel: string | null; puntosParaSiguiente: number;
  sesionesCompletadas: number; proximaReserva: ReservaOps | null;
  cancelacionesMes: number; limiteCancelacionesMes: number; horasMinimasCancelacion: number;
}

// ---- Fichas de clientes (terapeutas) ----
export interface MiClienteOps {
  clienteId: number; nombre: string; telefono: string | null; sesiones: number;
  ultimaSesion: string | null; proximaSesion: string | null; tieneFicha: boolean;
}
export interface NotaSesionOps {
  id: number; fecha: string; terapeuta: string; tratamiento: string;
  observaciones: string | null; recomendaciones: string | null; reservaId: number | null; propia: boolean;
}
export interface FichaOps {
  clienteId: number; clienteNombre: string; telefono: string | null;
  alergias: string | null; condiciones: string | null; presionPreferida: string | null; aceites: string | null;
  zonasRestringidas: string | null; notas: string | null; actualizadoPor: string | null; actualizadoEn: string | null;
  notasSesion: NotaSesionOps[];
}
export interface FichaForm {
  alergias: string; condiciones: string; presionPreferida: string; aceites: string; zonasRestringidas: string; notas: string;
}

// ---- Sistema y seguridad (ADMIN) ----
export interface AuditoriaOps {
  id: number; fecha: string; usuario: string | null; rol: string | null; accion: string;
  recurso: string | null; estado: number | null; ip: string | null; detalle: string | null;
}
export interface EstadoSistemaOps {
  version: string; java: string; springBoot: string; iniciadoEn: string; segundosActivo: number;
  baseDatosActiva: boolean; baseDatos: string; memoriaUsadaMb: number; memoriaMaxMb: number;
  registros: Record<string, number>; reservasPorEstado: Record<string, number>; usuariosPorRol: Record<string, number>;
  cicloUltimaEjecucion: string | null; cicloUltimosCambios: number; cicloTotalCambios: number; intentosDenegados24h: number;
}
export interface PermisoOps { accion: string; metodo: string; ruta: string; roles: string[]; nota: string; }

// ---- Accesos del personal (ADMIN) ----
export interface UsuarioOps {
  id: number; correo: string; nombre: string; rol: string; activo: boolean; debeCambiarClave: boolean;
  vinculo: string; empleadoId: number | null;
}
export interface UsuarioNuevoOps { usuario: UsuarioOps; claveTemporal: string; }

// ---- Perfil de la terapeuta ----
export interface MiPerfilOps { id: number; nombre: string; especialidad: string | null; telefono: string | null; bio: string | null; tieneFoto: boolean; }

// ---- Puntos y canjes ----
export interface MovimientoPuntos { fecha: string; tipo: 'GANADO' | 'CANJE'; descripcion: string; puntos: number; }
export interface BeneficioPuntos { id: string; titulo: string; descripcion: string; puntos: number; descuento: number; alcanza: boolean; }
export interface NivelPuntos { nombre: string; desde: number; actual: boolean; }
export interface CanjeOps {
  codigo: string; titulo: string; descuento: number; estado: 'DISPONIBLE' | 'USADO' | 'ANULADO' | 'VENCIDO';
  creadoEn: string; usadoEn: string | null; venceEn: string | null; anulable: boolean;
}
export interface PuntosOps {
  saldo: number; ganados: number; canjeados: number; nivel: string; siguienteNivel: string | null; puntosParaSiguiente: number;
  reglas: string[]; niveles: NivelPuntos[]; beneficios: BeneficioPuntos[]; movimientos: MovimientoPuntos[]; canjes: CanjeOps[];
}
export interface CanjeValidoOps {
  codigo: string; titulo: string; cliente: string; descuento: number; estado: string; valido: boolean; motivo: string | null;
  precio: number; montoACobrar: number;
}
