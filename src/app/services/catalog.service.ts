import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ServiceItem } from '../models/service.model';
import { Professional } from '../models/professional.model';
import { SpaceItem } from '../models/space.model';

export interface VideoReel {
  id: string;
  title: string;
  subtitle: string;
  category: string;
  videoUrl: string;
  posterUrl?: string;
  tag: string;
}

@Injectable({
  providedIn: 'root'
})
export class CatalogService {

  private services: ServiceItem[] = [
    {
      id: 'srv-01',
      code: '🫧 BUBBLE 01',
      name: 'Bubble Hydro Glow Facial & Oxygen',
      category: 'FACIAL',
      description: 'Limpieza hidrofacial profunda con micro-burbujas activas oxigenadas, sérum iluminador de frambuesa silvestre y mascarilla jelly de colágeno rosa.',
      durationMinutes: 60,
      price: 130,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    },
    {
      id: 'srv-02',
      code: '🫧 BUBBLE 02',
      name: 'Bubble Berry Relax & Deep Massage',
      category: 'MASAJE',
      description: 'Masaje relajante y descontracturante con aceites esenciales tibios de fresa & chicle, ventosas de succión suave bubble y piedras de cuarzo rosa.',
      durationMinutes: 75,
      price: 150,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    },
    {
      id: 'srv-03',
      code: '🫧 BUBBLE 03',
      name: 'Bubble Pop Podología Spa & Jelly Bath',
      category: 'PODOLOGÍA',
      description: 'Pedicura spa con baño efervescente de sales aromatizadas Bubble Pop, remoción suave de asperezas, exfoliación de azúcar rosa y pulido de uñas gloss.',
      durationMinutes: 50,
      price: 85,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    },
    {
      id: 'srv-04',
      code: '🫧 BUBBLE 04',
      name: 'Bubble Cloud Drenaje Linfático & Preso',
      category: 'CORPORAL',
      description: 'Drenaje linfático manual y presoterapia suave en cámaras de compresión neumática efecto nube para alivio total de piernas y reactivación circulatoria.',
      durationMinutes: 60,
      price: 120,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: true
    },
    {
      id: 'srv-05',
      code: '🫧 BUBBLE 05',
      name: 'Bubble Sculpt Pink Maderoterapia',
      category: 'CORPORAL',
      description: 'Modelado y reducción corporal de alta precisión con copas de burbuja modeladoras, maderoterapia y gel criogénico reafirmante de sandía.',
      durationMinutes: 80,
      price: 160,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: false
    },
    {
      id: 'srv-06',
      code: '🫧 BUBBLE 06',
      name: 'Bubble Luxe Foot & Parafina Glow',
      category: 'PODOLOGÍA',
      description: 'Ritual podológico intensivo con baño de burbujas emolientes, hidratación profunda con botitas de parafina tibia rosa y masaje relajante plantar.',
      durationMinutes: 55,
      price: 95,
      imageUrl: 'assets/Krea2-155101_00001_.png',
      featured: false
    }
  ];

  private videoReels: VideoReel[] = [
    {
      id: 'reel-01',
      title: 'Bubble Hydro Facial Experiencia',
      subtitle: 'Oxigenación profunda & Glow Instantáneo',
      category: 'FACIAL',
      videoUrl: 'videos/bubble-facial.mp4',
      posterUrl: 'assets/Krea2-155013_00001_.png',
      tag: '✨ MÁS POPULAR'
    },
    {
      id: 'reel-02',
      title: 'Bubble Berry Relax Massage',
      subtitle: 'Alivio de tensiones & Aromaterapia Dulce',
      category: 'MASAJE',
      videoUrl: 'videos/bubble-massage.mp4',
      posterUrl: 'assets/Krea2-155101_00001_.png',
      tag: '💖 RELAX TOTAL'
    },
    {
      id: 'reel-03',
      title: 'Bubble Pop Pedicura & Jelly Spa',
      subtitle: 'Efervescencia relajante & Pies de Seda',
      category: 'PODOLOGÍA',
      videoUrl: 'videos/bubble-pedi.mp4',
      posterUrl: 'assets/Krea2-194016_00002_.png',
      tag: '🫧 SPA FEET'
    },
    {
      id: 'reel-04',
      title: 'Bubble Suites & Pink Sanctuary',
      subtitle: 'Ambiente sensorial inmersivo 100% privado',
      category: 'SANCTUARY',
      videoUrl: 'videos/bubble-space.mp4',
      posterUrl: 'assets/Krea2-155023_00001_.png',
      tag: '🌸 SUITE VIP'
    },
    {
      id: 'reel-05',
      title: 'Bubble Signature Rituals',
      subtitle: 'Protocolos exclusivos de bienestar Bubblegum',
      category: 'WELLNESS',
      videoUrl: 'videos/bubble-hero.mp4',
      posterUrl: 'assets/Krea2-155034_00001_.png',
      tag: '👑 SIGNATURE'
    }
  ];

  private professionals: Professional[] = [
    {
      id: 'prof-01',
      name: 'Valentina Ross',
      title: 'Especialista en Bubble Facial & Dermoestética',
      specialty: 'Hidrofaciales de Oxígeno & Mascarillas Jelly Glow',
      photoUrl: 'assets/Krea2-154110_00001_.png',
      bio: 'Pionera en tratamientos hidrofaciales con microburbujas oxigenadas y cosmética sensorial con extractos frutales.',
      duties: 'Limpieza con micro-burbujas, exfoliación enzimática de frambuesa y terapia LED de colágeno.',
      availability: 'Disponible hoy',
      category: 'FACIAL' as any
    },
    {
      id: 'prof-02',
      name: 'Freya Lind',
      title: 'Terapeuta Corporal & Masajes Bubble Relax',
      specialty: 'Liberación de Estrés, Aceites Esenciales & Cuarzo Rosa',
      photoUrl: 'assets/Krea2-193952_00001_.png',
      bio: 'Experta en maniobras de relajación envolvente, aromaterapia Bubble Pop y masajes sensitivos descontracturantes.',
      duties: 'Masajes con bálsamos tibios, ventosas suaves y piedras de cuarzo rosa energizantes.',
      availability: 'Disponible hoy',
      category: 'MASAJE'
    },
    {
      id: 'prof-03',
      name: 'Astrid Vane',
      title: 'Especialista en Escultura Corporal & Presoterapia',
      specialty: 'Bubble Sculpting & Drenaje Cloud Nube',
      photoUrl: 'assets/Krea2-194000_00001_.png',
      bio: 'Certificada en maderoterapia estética y protocolos de presoterapia de compresión secuencial para ligereza corporal.',
      duties: 'Modelado con copas de burbuja, presoterapia relajante y geles criogénicos.',
      availability: 'Turnos esta semana',
      category: 'CORPORAL' as any
    },
    {
      id: 'prof-04',
      name: 'Elena Roth',
      title: 'Podóloga Clínica & Especialista Bubble Feet',
      specialty: 'Pedicura Spa Efervescente & Pulido Gloss',
      photoUrl: 'assets/Krea2-194009_00001_.png',
      bio: 'Líder en podología estética y tratamientos emolientes con sales de baño burbujeantes y mascarillas regeneradoras.',
      duties: 'Cuidado podológico completo, exfoliación de azúcar rosa y acabado gloss brillante.',
      availability: 'Disponible hoy',
      category: 'PODOLOGÍA'
    },
    {
      id: 'prof-05',
      name: 'Chloe Thorne',
      title: 'Especialista en Parafina Rosa & Hidratación',
      specialty: 'Tratamientos Térmicos & Cuidado Intensivo Plantar',
      photoUrl: 'assets/Krea2-194016_00002_.png',
      bio: 'Enfocada en nutrición tisular profunda mediante baños emolientes y botitas térmicas de parafina rosa aromática.',
      duties: 'Baño de burbujas termales, mascarillas lipídicas y masaje relajante plantar.',
      availability: 'Turnos esta semana',
      category: 'PODOLOGÍA'
    }
  ];

  private spaces: SpaceItem[] = [
    {
      id: 'spc-01',
      code: '🌸 SUITE 01',
      name: 'Pink Cloud Sanctum',
      subtitle: 'Cámara de relajación & masajes sensoriales',
      description: 'Equipada con camilla térmica de viscoelástica, cielo de luces led rosadas y difusor aromático de frambuesa dulce.',
      features: ['Camilla Térmica Suave', 'Cromoterapia Rosa Pastel', 'Aromaterapia Bubblegum'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-02',
      code: '🫧 SUITE 02',
      name: 'Hydro Bubble Lounge',
      subtitle: 'Gabinete de Podología Spa & Jelly Bath',
      description: 'Sillones anatómicos con hidromasaje de microburbujas, luz cálida envolvente e infusiones refrescantes.',
      features: ['Sillón Hidro-Masaje 360°', 'Tina de Burbujas Efervescentes', 'Pantalla Relax Streaming'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-03',
      code: '✨ SUITE 03',
      name: 'Oxygen & Glow Vault',
      subtitle: 'Estudio de oxigenoterapia facial',
      description: 'Entorno esterilizado de estética avanzada con tecnología de microburbujas de oxígeno y terapia de luz LED.',
      features: ['Generador de Oxígeno Puro', 'Máscara LED Multifrecuencia', 'Aislamiento Acústico Total'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1 Persona'
    },
    {
      id: 'spc-04',
      code: '💖 SUITE 04',
      name: 'Berry Dream Twin Suite',
      subtitle: 'Suite dúo para experiencias simultáneas',
      description: 'Espacio VIP amplio con dos camillas de tratamiento, batas de seda rosa y servicio exclusivo de mocktails Bubble Pop.',
      features: ['Atención Dúo Simultánea', 'Lounge Privado & Vestidor', 'Barra Mocktails Efervescentes'],
      imageUrl: 'assets/Krea2-155013_00001_.png',
      capacity: '1-2 Personas'
    }
  ];

  getFeaturedServices(): Observable<ServiceItem[]> {
    return of(this.services);
  }

  getServicesByCategory(category?: string): Observable<ServiceItem[]> {
    if (!category || category === 'ALL') return of(this.services);
    return of(this.services.filter(s => s.category.toUpperCase() === category.toUpperCase()));
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

  getVideoReels(): Observable<VideoReel[]> {
    return of(this.videoReels);
  }
}
