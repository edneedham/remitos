/** Shared shape for GET /auth/me/entitlement */
export type Entitlement = {
  can_download_app: boolean;
  subscription_plan?: string;
  trial_ends_at?: string;
  subscription_expires_at?: string;
  company_status?: string;
  archived_at?: string;
  warehouse_count?: number;
  device_count?: number;
  remitos_processed_last_30_days?: number;
  warehouse_usage_last_30_days?: Array<{
    warehouse_id: string;
    name: string;
    count: number;
  }>;
  documents_monthly_limit?: number;
  documents_usage_mtd?: number;
  documents_usage_series?: Array<{ date: string; cumulative: number }>;
  documents_usage_by_warehouse_mtd?: Array<{
    warehouse_id: string;
    name: string;
    count: number;
  }>;
};

export type BillingInvoiceRow = {
  id: string;
  amount_minor: number;
  currency: string;
  status: string;
  description?: string;
  issued_at: string;
  mp_payment_id?: string;
};
