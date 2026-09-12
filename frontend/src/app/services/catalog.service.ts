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

export interface ReviewItem {
  id: string;
  clientName: string;
  stars: number;
  comment: string;
  serviceName?: string;
  masseuseName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CatalogService {

  private services: ServiceItem[] = [
    {
      id: 'rc-01',
      code: 'RC-01',
      name: 'Masaje Tántrico Sensitivo',
      category: 'MASAJE',
      description: 'Toques lentos, suaves y continuos por todo el cuerpo con aceites tibios neutros. Máxima relajación muscular y estimulación sensorial de pies a cabeza.',
      durationMinutes: 60,
      price: 200,
      imageUrl: 'assets/img/services/rc-01.jpg',
      featured: true
    },
    {
      id: 'rc-02',
      code: 'RC-02',
      name: 'Masaje Nuru Body Slide',
      category: 'MASAJE',
      description: 'Masaje cuerpo a cuerpo sobre camilla especial con gel nuru tibio hiperdeslizante. Deslizamientos completos y contacto continuo.',
      durationMinutes: 75,
      price: 280,
      imageUrl: 'assets/img/services/rc-02.jpg',
      featured: true
    },
    {
      id: 'rc-03',
      code: 'RC-03',
      name: 'Masaje Relajante & Descontracturante',
      category: 'MASAJE',
      description: 'Presión media y profunda para liberar sobrecargas en espalda, hombros y cuello. Termina con toques suaves de descanso.',
      durationMinutes: 60,
      price: 180,
      imageUrl: 'assets/img/services/rc-03.jpg',
      featured: true
    },
    {
      id: 'rc-04',
      code: 'RC-04',
      name: 'Masaje Tántrico Completo',
      category: 'MASAJE',
      description: 'Sesión tántrica integral que combina maniobras sensitivas lentas con piedras tibias de cuarzo y aceites aromáticos.',
      durationMinutes: 75,
      price: 250,
      imageUrl: 'assets/img/services/rc-04.jpg',
      featured: true
    },
    {
      id: 'rc-05',
      code: 'RC-05',
      name: 'Masaje a Cuatro Manos',
      category: 'MASAJE',
      description: 'Dos masajistas trabajando al mismo tiempo de manera sincronizada. Relajación total e intensa.',
      durationMinutes: 60,
      price: 380,
      imageUrl: 'assets/img/services/rc-05.jpg',
      featured: true
    },
    {
      id: 'rc-06',
      code: 'RC-06',
      name: 'Masaje RECOVR VIP',
      category: 'MASAJE',
      description: 'La sesión más completa: masaje tántrico, técnica cuerpo a cuerpo Nuru y tiempo de tina de hidromasaje privada con microburbujas.',
      durationMinutes: 90,
      price: 450,
      imageUrl: 'assets/img/services/rc-06.jpg',
      featured: true
    }
  ];

  private videoReels: VideoReel[] = [
    {
      id: 'reel-01',
      title: 'Masaje Tántrico Sensitivo',
      subtitle: 'Toques lentos y estimulación suave con aceites tibios',
      category: 'TÁNTRICO',
      videoUrl: 'videos/bubble-facial.mp4',
      posterUrl: 'assets/img/services/rc-01.jpg',
      tag: '✨ SENSITIVO'
    },
    {
      id: 'reel-02',
      title: 'Masaje Nuru Body Slide',
      subtitle: 'Deslizamientos continuos y contacto cuerpo a cuerpo',
      category: 'NURU',
      videoUrl: 'videos/bubble-massage.mp4',
      posterUrl: 'assets/img/services/rc-02.jpg',
      tag: '💖 NURU SLIDE'
    },
    {
      id: 'reel-03',
      title: 'Masaje a Cuatro Manos',
      subtitle: 'Sincronización simultánea de dos masajistas',
      category: '4 MANOS',
      videoUrl: 'videos/4manos.mp4',
      posterUrl: 'assets/img/services/rc-05.jpg',
      tag: '✨ 4 MANOS'
    },
    {
      id: 'reel-04',
      title: 'Cabinas Privadas & Ambiente Neón',
      subtitle: 'Privacidad absoluta y cabinas climatizadas',
      category: 'CABINAS',
      videoUrl: 'videos/bubble-space.mp4',
      posterUrl: 'assets/img/cabinapriv.jpg',
      tag: '🌸 CABINAS'
    },
    {
      id: 'reel-05',
      title: 'Experiencia RECOVR VIP',
      subtitle: 'Masaje completo con tina de hidromasaje privada',
      category: 'VIP',
      videoUrl: 'videos/bubble-hero.mp4',
      posterUrl: 'assets/img/services/rc-06.jpg',
      tag: '👑 SESIÓN VIP'
    }
  ];

  private professionals: Professional[] = [
    {
      id: 'prof-01',
      name: 'Ely',
      title: 'Especialista en Masaje Tántrico Sensitivo',
      specialty: 'Masaje Tántrico Sensitivo y técnicas suaves',
      photoUrl: 'assets/img/ely.png',
      videoUrl: 'videos/ely.mp4',
      bio: 'Especialista en toques lentos, continuos y suaves por todo el cuerpo con aceites tibios neutros.',
      duties: 'Estimulación sensorial de pies a cabeza y relajación muscular profunda en ambiente tenue.',
      availability: 'Disponible hoy',
      category: 'MASAJE'
    },
    {
      id: 'prof-02',
      name: 'Miranda',
      title: 'Experta en Masaje Nuru Body Slide',
      specialty: 'Masaje Nuru Body Slide y cuerpo a cuerpo',
      photoUrl: 'assets/img/miranda.png',
      videoUrl: 'videos/mira.mp4',
      bio: 'Especialista en técnicas cuerpo a cuerpo sobre camilla especial con gel nuru tibio hiperdeslizante.',
      duties: 'Deslizamientos corporales completos, contacto continuo y descanso integral.',
      availability: 'Disponible hoy',
      category: 'MASAJE'
    },
    {
      id: 'prof-03',
      name: 'Pamela',
      title: 'Especialista en Descontracturante & Cuatro Manos',
      specialty: 'Descontracturante profundo y Masajes a Cuatro Manos',
      photoUrl: 'assets/img/pame.png',
      videoUrl: 'videos/sadie.mp4',
      bio: 'Experta en presión media y profunda para liberar sobrecargas musculares y maniobras sincronizadas simultáneas.',
      duties: 'Descompresión de espalda, hombros y cuello, y sesiones dobles a cuatro manos.',
      availability: 'Turnos por agenda',
      category: 'MASAJE'
    },
    {
      id: 'prof-04',
      name: 'Maria',
      title: 'Atención en Masaje RECOVR VIP',
      specialty: 'Masaje RECOVR VIP y sesiones completas combinadas',
      photoUrl: 'assets/img/maria.png',
      videoUrl: 'videos/maria.mp4',
      bio: 'Atención exclusiva en la sesión más completa: masaje tántrico, técnica Nuru cuerpo a cuerpo e hidromasaje.',
      duties: 'Sesiones integrales VIP con tiempo de tina de hidromasaje privada en suite.',
      availability: 'Disponible hoy',
      category: 'MASAJE'
    }
  ];

  private spaces: SpaceItem[] = [
    {
      id: 'spc-01',
      code: 'CABINA 01',
      name: 'Néon Rose',
      subtitle: 'Cabina privada con iluminación neón tenue',
      description: 'Camilla térmica extra ancha, sábanas satinadas y control de iluminación neón.',
      features: ['Camilla Térmica Extra Ancha', 'Sábanas Satinadas', 'Control de Iluminación Neón'],
      imageUrl: 'assets/img/cabinapriv.jpg',
      capacity: '1 Persona'
    },
    {
      id: 'spc-02',
      code: 'CABINA 02',
      name: 'Nuru Suite',
      subtitle: 'Especial para masaje cuerpo a cuerpo',
      description: 'Especial para cuerpo a cuerpo, equipada con colchón impermeable y ducha privada de alta presión.',
      features: ['Colchón Impermeable Nuru', 'Ducha Privada Alta Presión', 'Climatización Óptima'],
      imageUrl: 'assets/img/cabinapriv.jpg',
      capacity: '1 Persona'
    },
    {
      id: 'spc-03',
      code: 'CABINA 03',
      name: 'Dúo / 4 Manos',
      subtitle: 'Espacio amplio para sesiones simultáneas',
      description: 'Espacio amplio para sesiones simultáneas con dos masajistas trabajando de manera sincronizada.',
      features: ['Sesiones a Cuatro Manos', 'Espacio Amplio Climatizado', 'Sonido Acústico Envolvente'],
      imageUrl: 'assets/img/cabinapriv.jpg',
      capacity: '1-2 Personas'
    },
    {
      id: 'spc-04',
      code: 'CABINA 04',
      name: 'VIP Hidromasaje',
      subtitle: 'Camilla especial y tina de hidromasaje privada',
      description: 'Camilla especial y tina de hidromasaje privada en suite con microburbujas para relajación total.',
      features: ['Tina de Hidromasaje Privada', 'Microburbujas Relajantes', 'Máxima Exclusividad & Confort'],
      imageUrl: 'assets/img/cabinapriv.jpg',
      capacity: '1 Persona'
    }
  ];

  private reviews: ReviewItem[] = [
    {
      id: 'rev-01',
      clientName: 'Carlos M.',
      stars: 5,
      comment: 'Excelente nivel de privacidad y atención impecable desde la recepción. El masaje Nuru con Miranda superó totalmente mis expectativas. La cabina climatizada y la música de fondo hacen que te desconectes al 100%.',
      serviceName: 'Masaje Nuru Body Slide',
      masseuseName: 'Miranda'
    },
    {
      id: 'rev-02',
      clientName: 'Diego R.',
      stars: 5,
      comment: 'El masaje tántrico sensitivo con Ely es otro nivel. Muy profesional, el ambiente con luz tenue neón es súper relajante y los aceites tibios marcan la diferencia. Definitivamente volveré.',
      serviceName: 'Masaje Tántrico Sensitivo',
      masseuseName: 'Ely'
    },
    {
      id: 'rev-03',
      clientName: 'Andrés V.',
      stars: 5,
      comment: 'Probé el masaje a cuatro manos con Pamela y fue una experiencia increíblemente relajante. Local discreto, limpio y súper puntual. 10 de 10.',
      serviceName: 'Masaje a Cuatro Manos',
      masseuseName: 'Pamela'
    },
    {
      id: 'rev-04',
      clientName: 'Javier S.',
      stars: 5,
      comment: 'Instalaciones de primera. La tina de hidromasaje privada en la cabina VIP es genial después de una semana de estrés. Excelente servicio de Maria.',
      serviceName: 'Masaje RECOVR VIP',
      masseuseName: 'Maria'
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
    return of(this.services.find(s => s.id.toLowerCase() === id.toLowerCase() || s.code.toLowerCase() === id.toLowerCase() || s.name.toLowerCase().includes(id.toLowerCase())));
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

  getReviews(): Observable<ReviewItem[]> {
    return of(this.reviews);
  }
}
