export type ServiceCategory = 'MASAJE' | 'PODOLOGÍA';

export interface ServiceItem {
  id: string;
  code: string; // e.g. '[ PROTOCOLO 01 ]'
  name: string;
  category: ServiceCategory;
  description: string;
  durationMinutes: number;
  price: number;
  imageUrl: string;
  featured?: boolean;
}
