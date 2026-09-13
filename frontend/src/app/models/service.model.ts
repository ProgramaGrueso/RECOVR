export type ServiceCategory = 'TÁNTRICO' | 'NURU' | 'DESCONTRACTURANTE' | '4 MANOS' | 'VIP' | 'MASAJE' | 'FACIAL' | 'ALL' | string;

export interface ServiceItem {
  id: string;
  code: string; // e.g. 'RC-01'
  name: string;
  category: ServiceCategory;
  description: string;
  durationMinutes: number;
  price: number;
  imageUrl: string;
  videoUrl?: string;
  posterUrl?: string;
  tag?: string;
  features?: string[];
  featured?: boolean;
}
