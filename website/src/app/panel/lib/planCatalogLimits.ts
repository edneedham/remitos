/** GET /auth/me/plan-catalog-limits — mirrors billing.PlanLimitsByID */
export type PlanLimitRow = {
  max_warehouses?: number;
  max_users?: number;
  documents_monthly_limit?: number;
};

export type PlanCatalogLimitsResponse = {
  plans: Record<string, PlanLimitRow>;
};
