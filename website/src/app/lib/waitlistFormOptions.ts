/** Values sent to POST /public/waitlist (must match backend oneof). */
export const DELIVERY_NOTES_PER_DAY_VALUES = [
  'lt10',
  'lt50',
  'lt200',
  'gt200',
] as const;

export type DeliveryNotesPerDayValue =
  (typeof DELIVERY_NOTES_PER_DAY_VALUES)[number];

export const DELIVERY_NOTES_PER_DAY_OPTIONS: {
  value: DeliveryNotesPerDayValue;
  label: string;
}[] = [
  // API values stay lt10/lt50/lt200/gt200; labels are mutually exclusive ranges (no overlapping “menos de”).
  { value: 'lt10', label: '0 a 9 remitos por día' },
  { value: 'lt50', label: '10 a 49 remitos por día' },
  { value: 'lt200', label: '50 a 199 remitos por día' },
  { value: 'gt200', label: '200 o más por día' },
];

export const PROCESSING_MODE_VALUES = ['digital', 'manual'] as const;
export type ProcessingModeValue = (typeof PROCESSING_MODE_VALUES)[number];

export const PROCESSING_MODE_OPTIONS: {
  value: ProcessingModeValue;
  label: string;
}[] = [
  {
    value: 'digital',
    label: 'En digital (si querés, indicá qué aplicación usás abajo)',
  },
  { value: 'manual', label: 'Manual / en papel' },
];

/** Max lengths aligned with POST /public/waitlist (backend validation). */
export const WAITLIST_FIELD_MAX = {
  email: 254,
  fullName: 200,
  companyName: 200,
  digitalApplication: 200,
  logisticsPainPoints: 2000,
  /** Digits only; max value 50_000 fits in 5 digits. */
  warehouseDigits: 5,
} as const;
