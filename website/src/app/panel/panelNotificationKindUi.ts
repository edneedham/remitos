import type { LucideIcon } from 'lucide-react';
import {
  AlertCircle,
  AlertTriangle,
  Ban,
  BarChart3,
  Bell,
  CalendarClock,
  CreditCard,
  FileCheck,
  FileWarning,
  Hourglass,
  Layers,
  Link2,
  Receipt,
  ScanLine,
  Smartphone,
  Sparkles,
  UserPlus,
} from 'lucide-react';

/** Mirrors `UserNotificationKind` in the API; used for icon + tone only (titles come from the API). */
export type PanelNotificationKind =
  | 'signup_welcome'
  | 'renewal_charge_failed'
  | 'invoice_paid'
  | 'subscription_renewal_upcoming'
  | 'trial_ending_soon'
  | 'subscription_lapsed'
  | 'plan_changed'
  | 'factura_ready'
  | 'factura_failed'
  | 'device_registered'
  | 'device_revoked'
  | 'device_reactivated'
  | 'session_transfer_completed'
  | 'operator_created'
  | 'payment_method_updated'
  | 'documents_usage_warning'
  | 'first_scan_completed';

export type PanelNotificationTone =
  | 'neutral'
  | 'positive'
  | 'warning'
  | 'negative';

export type PanelNotificationKindUi = {
  Icon: LucideIcon;
  tone: PanelNotificationTone;
};

export const PANEL_NOTIFICATION_TONE_WRAP: Record<
  PanelNotificationTone,
  string
> = {
  neutral: 'bg-gray-100 text-gray-600',
  positive: 'bg-emerald-50 text-emerald-700',
  warning: 'bg-amber-50 text-amber-700',
  negative: 'bg-red-50 text-red-700',
};

const KIND_UI: Record<PanelNotificationKind, PanelNotificationKindUi> = {
  signup_welcome: { Icon: Sparkles, tone: 'positive' },
  renewal_charge_failed: { Icon: AlertCircle, tone: 'negative' },
  invoice_paid: { Icon: Receipt, tone: 'positive' },
  subscription_renewal_upcoming: { Icon: CalendarClock, tone: 'neutral' },
  trial_ending_soon: { Icon: Hourglass, tone: 'warning' },
  subscription_lapsed: { Icon: AlertTriangle, tone: 'negative' },
  plan_changed: { Icon: Layers, tone: 'positive' },
  factura_ready: { Icon: FileCheck, tone: 'positive' },
  factura_failed: { Icon: FileWarning, tone: 'warning' },
  device_registered: { Icon: Smartphone, tone: 'neutral' },
  device_revoked: { Icon: Ban, tone: 'warning' },
  device_reactivated: { Icon: Smartphone, tone: 'positive' },
  session_transfer_completed: { Icon: Link2, tone: 'neutral' },
  operator_created: { Icon: UserPlus, tone: 'neutral' },
  payment_method_updated: { Icon: CreditCard, tone: 'neutral' },
  documents_usage_warning: { Icon: BarChart3, tone: 'warning' },
  first_scan_completed: { Icon: ScanLine, tone: 'positive' },
};

/** Icon + tone for the notification list row; unknown kinds fall back to a neutral bell. */
export function panelNotificationKindUi(kind: string): PanelNotificationKindUi {
  const mapped = KIND_UI[kind as PanelNotificationKind];
  if (mapped) return mapped;
  return { Icon: Bell, tone: 'neutral' };
}
