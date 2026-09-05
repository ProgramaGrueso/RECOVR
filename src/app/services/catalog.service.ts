import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { ServiceItem } from '../models/service.model';
import { Professional } from '../models/professional.model';
import { SpaceItem } from '../models/space.model';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {

  private services: ServiceItem[] = [
    {
      id: 'srv-01',
      code: '[ PROTOCOLO 01 ]',
      name: 'Descompresión Miofascial Profunda',
      category: 'MASAJE',
      description: 'Liberación de tensión muscular intensa dirigida a dorsales, trapecios y cuadriceps. Terapia combinada con bálsamos térmicos y ventosas frías.',
      durationMinutes: 75,
      price: 65,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    },
    {
      id: 'srv-02',
      code: '[ PROTOCOLO 02 ]',
      name: 'Podología Clínica & Reconstructiva',
      category: 'PODOLOGÍA',
      description: 'Diagnóstico plantar, remoción de hiperqueratosis, corte clínico quirúrgico y tratamiento de uñas encarnadas con sellado antiséptico.',
      durationMinutes: 60,
      price: 55,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    },
    {
      id: 'srv-03',
      code: '[ PROTOCOLO 03 ]',
      name: 'Masaje Deportivo & Descarga Articular',
      category: 'MASAJE',
      description: 'Protocolo de alta presión para atletas. Enfoque en flexibilidad cadera-rodilla, estiramientos asistidos y fricción de tejido profundo.',
      durationMinutes: 90,
      price: 80,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    },
    {
      id: 'srv-04',
      code: '[ PROTOCOLO 04 ]',
      name: 'Perfilado Podológico & Hidratación Profunda',
      category: 'PODOLOGÍA',
      description: 'Aseo podológico especializado con exfoliación de sales volcánicas y máscara lipídica para pies expuestos a alta carga calzada.',
      durationMinutes: 50,
      price: 48,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    }
  ];

  private professionals: Professional[] = [
    {
      id: 'prof-01',
      name: 'Valentina Ross',
      title: 'Kinesióloga Deportiva & Terapia Miofascial',
      specialty: 'Descompresión de Espalda & Liberación Tisular',
      photoUrl: 'assets/Krea2-154110_00001_.png',
      bio: 'Especialista en descompresión neuromuscular y regeneración de alto rendimiento con 8 años de trayectoria asistiendo a atletas de élite.',
      duties: 'Maniobras miofasciales de alta presión, termoterapia de contraste y alineación vertebral activa.',
      availability: 'Disponible hoy',
      category: 'MASAJE'
    },
    {
      id: 'prof-02',
      name: 'Freya Lind',
      title: 'Rehabilitación Funcional & Crioterapia',
      specialty: 'Recuperación Articular & Flexibilidad Asistida',
      photoUrl: 'assets/Krea2-193952_00001_.png',
      bio: 'Formada en centros escandinavos de medicina deportiva, experta en acondicionamiento articular y liberación de fascias profundas.',
      duties: 'Ajustes posturales, protocolos de flexibilidad asistida y masajes de descarga post-entrenamiento.',
      availability: 'Disponible hoy',
      category: 'MASAJE'
    },
    {
      id: 'prof-03',
      name: 'Astrid Vane',
      title: 'Kinesiología Miofascial & Ventosaterapia',
      specialty: 'Liberación Neuromuscular & Espalda Alta',
      photoUrl: 'assets/Krea2-194000_00001_.png',
      bio: 'Pionera en técnicas de ventosaterapia fría-térmica para la desinflamación y reactivación del flujo vascular y drenaje profundo.',
      duties: 'Ventosaterapia de vacío graduado, punción seca preventiva y relajación miofascial dorsal.',
      availability: 'Turnos esta semana',
      category: 'MASAJE'
    },
    {
      id: 'prof-04',
      name: 'Elena Roth',
      title: 'Podología Médica & Biomecánica Plantar',
      specialty: 'Podología Quirúrgica & Reconstrucción Ungueal',
      photoUrl: 'assets/Krea2-194009_00001_.png',
      bio: 'Líder en podología clínica y tratamiento biomecánico para pies expuestos a alta carga calzada e impacto de entrenamiento.',
      duties: 'Cirugía menor podológica, tratamiento de uñas encarnadas, quiropodología y reconstrucción ungueal.',
      availability: 'Disponible hoy',
      category: 'PODOLOGÍA'
    },
    {
      id: 'prof-05',
      name: 'Chloe Thorne',
      title: 'Podología Clínica & Acondicionamiento Tisular',
      specialty: 'Perfilado Podológico & Máscaras Lipídicas',
      photoUrl: 'assets/Krea2-194016_00002_.png',
      bio: 'Especializada en aseo podológico de grado quirúrgico y rejuvenecimiento de la lámina plantar mediante activos minerales.',
      duties: 'Exfoliación volcánica profunda, perfilado de hiperqueratosis e hidratación sellada de alta dermis.',
      availability: 'Turnos esta semana',
      category: 'PODOLOGÍA'
    }
  ];

  private spaces: SpaceItem[] = [
    {
      id: 'spc-01',
      code: '[ SUITE 01 ]',
      name: 'Sanctum Miofascial',
      subtitle: 'Cámara de descompresión profunda',
      description: 'Equipada con camilla térmica de jade y sistema de aislamiento acústico total para tratamientos corporales intensivos.',
      features: ['Camilla de Jade Térmica', 'Insonorización Activa 100%', 'Luz Regulable Biorritmo'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-02',
      code: '[ SUITE 02 ]',
      name: 'Podiatry Vault',
      subtitle: 'Gabinete de Podología Clínica',
      description: 'Estudio esterilizado de alta tecnología biomecánica con instrumental de grado médico y sillón anatómico automatizado.',
      features: ['Sillón Anatómico 360°', 'Instrumental Quirúrgico LED', 'Filtro HEPA Tisular'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-03',
      code: '[ SUITE 03 ]',
      name: 'Crioterapia & Penumbra',
      subtitle: 'Cámara de regeneración helada',
      description: 'Ambiente de temperatura subcero controlada e iluminación sombría diseñado para reducir inflamación muscular sistémica.',
      features: ['Criosauna Controlada', 'Monitoreo Cardíaco', 'Goteo Térmico Infuso'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-04',
      code: '[ SUITE 04 ]',
      name: 'Cámara de Presoterapia',
      subtitle: 'Unidad de compresión secuencial',
      description: 'Módulo neumático individual dedicado al drenaje linfático de extremidades inferiores y aceleración del retorno venoso.',
      features: ['Botas Compresivas 8 Cámaras', 'Terapia Normatec V2', 'Audio de Ondas Alfa'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-05',
      code: '[ SUITE 05 ]',
      name: 'Ritual Hidro-Térmico',
      subtitle: 'Gabinete de inmersión en sales',
      description: 'Tina de basalto negro con agua enriquecida en minerales volcánicos y sauna seco de cedro para eliminación de toxinas.',
      features: ['Tina Basalto Sulfatada', 'Sauna Cedro Oscuro', 'Ducha de Cascada Fría'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1-2 Personas'
    },
    {
      id: 'spc-06',
      code: '[ SUITE 06 ]',
      name: 'Electro-Estimulación',
      subtitle: 'Estudio de tonificación muscular',
      description: 'Radiofrecuencia y estimulación neuromuscular pasiva en un entorno confidencial de alta privacidad.',
      features: ['Generador Compex Pro', 'Radiofrecuencia Capacitiva', 'Ajuste Electrónico Precision'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-07',
      code: '[ SUITE 07 ]',
      name: 'Sanctuary Detox Aromático',
      subtitle: 'Espacio holístico de piedra volcánica',
      description: 'Sala envolvente aromatizada con extractos silvestres y piedras de basalto caliente para liberación de estrés nervioso.',
      features: ['Piedras Volcánicas Térmicas', 'Balsámicos Botánicos', 'Difusor Humo Frío'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-08',
      code: '[ SUITE 08 ]',
      name: 'VIP Master Private Loft',
      subtitle: 'Suite integral de cuerpo completo',
      description: 'La experiencia máxima RECOVR: suite privada ejecutiva con doble terapeuta, vestidor privado y ritual completo.',
      features: ['Atención Dúo Simultánea', 'Lounge & Vestidor Privado', 'Bar de Infusiones Adaptógenas'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1-2 Personas'
    }
  ];

  getFeaturedServices(): Observable<ServiceItem[]> {
    return of(this.services);
  }

  getServicesByCategory(category?: 'MASAJE' | 'PODOLOGÍA'): Observable<ServiceItem[]> {
    if (!category) return of(this.services);
    return of(this.services.filter(s => s.category === category));
  }

  getServiceById(id: string): Observable<ServiceItem | undefined> {
    return of(this.services.find(s => s.id === id || s.name.toLowerCase().includes(id.toLowerCase())));
  }

  getProfessionals(): Observable<Professional[]> {
    return of(this.professionals);
  }

  getSpaces(): Observable<SpaceItem[]> {
    return of(this.spaces);
  }
}



