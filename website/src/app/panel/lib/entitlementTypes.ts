/** Shared shape for GET /auth/me/entitlement */
export type Entitlement = {
  can_download_app: boolean;
  subscription_plan?: string;
  pending_plan?: string;
  trial_ends_at?: string;
  subscription_expires_at?: string;
  company_status?: string;
  archived_at?: string;
  warehouse_count?: number;
  max_warehouses?: number;
  device_count?: number;
  user_count?: number;
  max_users?: number;
  remitos_processed_last_30_days?: number;
  /** Server truth: at least one inbound note stored for the company (lifetime). */
  first_scan_completed?: boolean;
  /** UTC ISO timestamp of oldest inbound note when first_scan_completed. */
  first_scan_completed_at?: string;
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
  factura_tipo?: number;
  factura_pto_vta?: number;
  factura_numero?: number;
  factura_cae?: string | null;
  factura_cae_vto?: string | null;
  factura_emitted_at?: string | null;
  factura_pending?: boolean;
  factura_last_error?: string | null;
};
