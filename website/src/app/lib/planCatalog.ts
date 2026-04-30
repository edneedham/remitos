export type PlanCatalogItem = {
  id: 'pyme' | 'empresa' | 'corporativo';
  name: string;
  monthlyPriceLabel: string;
  description: string;
  perks: string[];
  overageLabel: string;
  featured?: boolean;
  customPricing?: boolean;
};

export const PLAN_CATALOG: PlanCatalogItem[] = [
  {
    id: 'pyme',
    name: 'PyME',
    monthlyPriceLabel: 'USD 29',
    description: 'Para equipos chicos que recién arrancan su operación.',
    perks: [
      '7 días de prueba incluidos',
      'Hasta 2 depósitos',
      '1 dispositivo por depósito',
      'Hasta 500 documentos/mes',
      'Soporte por email',
    ],
    overageLabel: 'USD 0.12 por documento adicional',
  },
  {
    id: 'empresa',
    name: 'Empresa',
    monthlyPriceLabel: 'USD 59',
    description: 'Para operaciones en crecimiento con más volumen diario.',
    perks: [
      '7 días de prueba incluidos',
      '3 depósitos',
      'Hasta 10 usuarios',
      'Hasta 10.000 documentos/mes',
      'Soporte prioritario',
    ],
    overageLabel: 'USD 0.09 por documento adicional',
    featured: true,
  },
  {
    id: 'corporativo',
    name: 'Corporativo',
    monthlyPriceLabel: 'A medida',
    description: 'Para empresas con múltiples sedes y requerimientos avanzados.',
    perks: [
      '7 días de prueba incluidos',
      'Depósitos ilimitados',
      'Usuarios ilimitados',
      'Límites y SLA a medida',
      'Acompañamiento dedicado',
    ],
    overageLabel: 'Excedentes a convenir',
    customPricing: true,
  },
];

export function getPlanById(planId?: string) {
  if (!planId) return null;
  return PLAN_CATALOG.find((p) => p.id === planId) ?? null;
}
