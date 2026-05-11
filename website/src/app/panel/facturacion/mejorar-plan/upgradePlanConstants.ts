/** Fallback until GET /auth/me/plan-catalog-limits loads — matches billing.PlanLimitsByID. */
export const PYME_LIMITS_FALLBACK = {
  warehouses: 2,
  users: 3,
  documentsMonthly: 500,
} as const;

export const MP_PUBLIC_KEY =
  typeof process !== 'undefined'
    ? (process.env.NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY ?? '').trim()
    : '';

export const USE_MOCK_PAYMENT =
  typeof process !== 'undefined' &&
  process.env.NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT === 'true';
