export type ServiceCategory = 'FACIAL' | 'MASAJE' | 'PODOLOGÍA' | 'CORPORAL' | 'ALL' | string;

export interface ServiceItem {
  id: string;
  code: string; // e.g. '🫧 BUBBLE 01'
  name: string;
  category: ServiceCategory;
  description: string;
  durationMinutes: number;
  price: number;
  imageUrl: string;
  videoUrl?: string;
  featured?: boolean;
}

