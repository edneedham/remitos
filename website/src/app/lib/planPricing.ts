/** GET /auth/me/plan-pricing — aligned with backend `planPricingResponse`. */
export type PlanPricingResponse = {
  plan_id: string;
  currency: string;
  amount_minor: number;
  monthly_list_usd: number;
  ars_per_usd: number;
  mep_ars_per_usd?: number;
  fx_buffer_fraction?: number;
  fx_source: string;
  fx_effective_date?: string;
  legal_notice_ar: string;
};
