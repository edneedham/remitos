package models

import (
	"encoding/json"
	"time"

	"github.com/google/uuid"
)

// UserNotificationKind identifies notification templates for routing and analytics.
const (
	UserNotificationKindSignupWelcome UserNotificationKind = "signup_welcome"

	UserNotificationKindRenewalChargeFailed UserNotificationKind = "renewal_charge_failed"
	UserNotificationKindInvoicePaid         UserNotificationKind = "invoice_paid"
	UserNotificationKindSubscriptionRenewalUpcoming UserNotificationKind = "subscription_renewal_upcoming"
	UserNotificationKindTrialEndingSoon     UserNotificationKind = "trial_ending_soon"
	UserNotificationKindSubscriptionLapsed  UserNotificationKind = "subscription_lapsed"
	UserNotificationKindPlanChanged         UserNotificationKind = "plan_changed"
	UserNotificationKindFacturaReady      UserNotificationKind = "factura_ready"
	UserNotificationKindFacturaFailed     UserNotificationKind = "factura_failed"
	UserNotificationKindDeviceRegistered  UserNotificationKind = "device_registered"
	UserNotificationKindDeviceRevoked     UserNotificationKind = "device_revoked"
	UserNotificationKindDeviceReactivated UserNotificationKind = "device_reactivated"
	UserNotificationKindSessionTransferCompleted UserNotificationKind = "session_transfer_completed"
	UserNotificationKindOperatorCreated   UserNotificationKind = "operator_created"
	UserNotificationKindPaymentMethodUpdated UserNotificationKind = "payment_method_updated"
	UserNotificationKindDocumentsUsageWarning UserNotificationKind = "documents_usage_warning"
	UserNotificationKindFirstScanCompleted UserNotificationKind = "first_scan_completed"
)

type UserNotificationKind string

type UserNotification struct {
	ID        uuid.UUID           `json:"id"`
	UserID    uuid.UUID           `json:"-"`
	CompanyID uuid.UUID           `json:"-"`
	Kind      UserNotificationKind `json:"kind"`
	Title     string              `json:"title"`
	Body      *string             `json:"body,omitempty"`
	ActionURL *string             `json:"action_url,omitempty"`
	ReadAt    *time.Time          `json:"read_at,omitempty"`
	Metadata  json.RawMessage     `json:"metadata,omitempty"`
	CreatedAt time.Time           `json:"created_at"`
}
