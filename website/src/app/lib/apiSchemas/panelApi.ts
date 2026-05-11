import { z } from 'zod';
import type { WebProfile } from '../webAuth';
import type { BillingInvoiceRow, Entitlement } from '../../panel/lib/entitlementTypes';
import type { PlanCatalogLimitsResponse } from '../../panel/lib/planCatalogLimits';

const warehouseUsagePoint = z.object({
  warehouse_id: z.string(),
  name: z.string(),
  count: z.number(),
});

const documentsUsagePoint = z.object({
  date: z.string(),
  cumulative: z.number(),
});

const documentsByWarehouse = z.object({
  warehouse_id: z.string(),
  name: z.string(),
  count: z.number(),
});

export const EntitlementSchema = z
  .object({
    can_download_app: z.boolean(),
    subscription_plan: z.string().optional(),
    pending_plan: z.string().optional(),
    trial_ends_at: z.string().optional(),
    subscription_expires_at: z.string().optional(),
    company_status: z.string().optional(),
    archived_at: z.string().optional(),
    warehouse_count: z.number().optional(),
    max_warehouses: z.number().optional(),
    device_count: z.number().optional(),
    user_count: z.number().optional(),
    max_users: z.number().optional(),
    remitos_processed_last_30_days: z.number().optional(),
    first_scan_completed: z.boolean().optional(),
    first_scan_completed_at: z.string().optional(),
    warehouse_usage_last_30_days: z.array(warehouseUsagePoint).optional(),
    documents_monthly_limit: z.number().optional(),
    documents_usage_mtd: z.number().optional(),
    documents_usage_series: z.array(documentsUsagePoint).optional(),
    documents_usage_by_warehouse_mtd: z.array(documentsByWarehouse).optional(),
  })
  .passthrough();

export const WebProfileSchema = z.object({
  id: z.string(),
  username: z.string(),
  email: z.string().optional(),
  company_id: z.string(),
  company_name: z.string(),
  company_code: z.string(),
  role: z.string(),
});

export const BillingInvoiceRowSchema = z.object({
  id: z.string(),
  amount_minor: z.number(),
  currency: z.string(),
  status: z.string(),
  description: z.string().optional(),
  issued_at: z.string(),
  mp_payment_id: z.string().optional(),
  factura_tipo: z.number().optional(),
  factura_pto_vta: z.number().optional(),
  factura_numero: z.number().optional(),
  factura_cae: z.string().nullable().optional(),
  factura_cae_vto: z.string().nullable().optional(),
  factura_emitted_at: z.string().nullable().optional(),
  factura_pending: z.boolean().optional(),
  factura_last_error: z.string().nullable().optional(),
});

export const PlanCatalogLimitsResponseSchema = z.object({
  plans: z.record(
    z.string(),
    z
      .object({
        max_warehouses: z.number().optional(),
        max_users: z.number().optional(),
        documents_monthly_limit: z.number().optional(),
      })
      .passthrough(),
  ),
});

export function parseWebProfile(input: unknown): WebProfile | null {
  const r = WebProfileSchema.safeParse(input);
  if (!r.success) return null;
  const d = r.data;
  return {
    id: d.id,
    username: d.username,
    email: d.email,
    company_id: d.company_id,
    company_name: d.company_name,
    company_code: d.company_code,
    role: d.role,
  };
}

export function parseEntitlement(input: unknown): Entitlement | null {
  const r = EntitlementSchema.safeParse(input);
  return r.success ? (r.data as Entitlement) : null;
}

export function parseBillingInvoiceList(input: unknown): BillingInvoiceRow[] | null {
  const r = z.array(BillingInvoiceRowSchema).safeParse(input);
  return r.success ? (r.data as BillingInvoiceRow[]) : null;
}

export function parsePlanCatalogLimits(
  input: unknown,
): PlanCatalogLimitsResponse | null {
  const r = PlanCatalogLimitsResponseSchema.safeParse(input);
  return r.success ? r.data : null;
}
