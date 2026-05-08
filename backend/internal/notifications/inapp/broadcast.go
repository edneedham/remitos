package inapp

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"
	"server/internal/logger"
	"server/internal/models"
	"server/internal/repository"
)

// Broadcaster inserts user_notifications for all web-panel users of a company.
type Broadcaster struct {
	Notif         *repository.UserNotificationRepository
	Users         *repository.UserRepository
	PublicSiteURL string
}

// NewBroadcaster returns nil if dependencies required for dispatch are missing.
func NewBroadcaster(notif *repository.UserNotificationRepository, users *repository.UserRepository, publicSiteURL string) *Broadcaster {
	if notif == nil || users == nil {
		return nil
	}
	return &Broadcaster{
		Notif:         notif,
		Users:         users,
		PublicSiteURL: strings.TrimSpace(publicSiteURL),
	}
}

func (b *Broadcaster) panelURL(suffix string) *string {
	base := strings.TrimRight(b.PublicSiteURL, "/")
	if base == "" {
		return nil
	}
	s := base + suffix
	return &s
}

func (b *Broadcaster) dispatch(
	ctx context.Context,
	companyID uuid.UUID,
	kind models.UserNotificationKind,
	title string,
	body *string,
	actionURL *string,
) {
	if b == nil || b.Notif == nil || b.Users == nil {
		return
	}
	ids, err := b.Users.ListWebPanelUserIDs(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Str("company_id", companyID.String()).Str("kind", string(kind)).Msg("inapp: list web users")
		return
	}
	now := time.Now().UTC()
	for _, uid := range ids {
		n := &models.UserNotification{
			ID:        uuid.New(),
			UserID:    uid,
			CompanyID: companyID,
			Kind:      kind,
			Title:     title,
			Body:      body,
			ActionURL: actionURL,
			CreatedAt: now,
		}
		if err := b.Notif.Insert(ctx, n); err != nil {
			logger.Log.Error().Err(err).Str("user_id", uid.String()).Str("kind", string(kind)).Msg("inapp: insert notification")
		}
	}
}

// RenewalChargeFailed mirrors renewal failure email (pending invoice).
func (b *Broadcaster) RenewalChargeFailed(ctx context.Context, companyID uuid.UUID, amountMinor int64, currency, reason string) {
	title := "No pudimos cobrar la renovación"
	body := fmt.Sprintf(
		"El cobro automático falló (%s). Actualizá el medio de pago en Facturación para evitar interrupciones.",
		strings.TrimSpace(reason),
	)
	if len(body) > 280 {
		body = body[:277] + "…"
	}
	b.dispatch(ctx, companyID, models.UserNotificationKindRenewalChargeFailed, title, strPtr(body), b.panelURL("/panel/facturacion"))
	_ = amountMinor
	_ = currency
}

// InvoicePaid notifies panel users when a paid invoice row exists (after payment settles).
func (b *Broadcaster) InvoicePaid(ctx context.Context, companyID uuid.UUID, amountMinor int64, currency string) {
	title := "Pago registrado"
	body := fmt.Sprintf("Registramos un pago de %s %.2f en tu cuenta.", strings.ToUpper(strings.TrimSpace(currency)), float64(amountMinor)/100.0)
	b.dispatch(ctx, companyID, models.UserNotificationKindInvoicePaid, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

func strPtr(s string) *string { return &s }

// SubscriptionRenewalUpcoming matches the renewal reminder email window (estimatedWholeARS is pesos enteros, same as email body).
func (b *Broadcaster) SubscriptionRenewalUpcoming(ctx context.Context, companyID uuid.UUID, companyName string, estimatedWholeARS int64) {
	title := "Tu suscripción renueva pronto"
	body := fmt.Sprintf(
		"%s: el próximo cobro estimado es de aprox. $%d ARS (según cotización). Revisá Facturación si necesitás actualizar la tarjeta.",
		strings.TrimSpace(companyName),
		estimatedWholeARS,
	)
	b.dispatch(ctx, companyID, models.UserNotificationKindSubscriptionRenewalUpcoming, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

// TrialEndingSoon mirrors the 3-day trial ending notice.
func (b *Broadcaster) TrialEndingSoon(ctx context.Context, companyID uuid.UUID, companyName, datePhrase string) {
	title := "Tu prueba está por terminar"
	body := fmt.Sprintf("%s: la prueba termina %s. Activá un plan en Facturación para no perder acceso.", strings.TrimSpace(companyName), strings.TrimSpace(datePhrase))
	b.dispatch(ctx, companyID, models.UserNotificationKindTrialEndingSoon, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

// SubscriptionLapsed mirrors lapse notice email.
func (b *Broadcaster) SubscriptionLapsed(ctx context.Context, companyID uuid.UUID, companyName, lapsePhrase string) {
	title := "Suscripción vencida"
	body := fmt.Sprintf("%s: tu período pagado finalizó (%s). Regularizá el pago en Facturación.", strings.TrimSpace(companyName), strings.TrimSpace(lapsePhrase))
	b.dispatch(ctx, companyID, models.UserNotificationKindSubscriptionLapsed, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

// PlanChanged fires after a successful plan upgrade/downgrade charge path.
func (b *Broadcaster) PlanChanged(ctx context.Context, companyID uuid.UUID, planID string) {
	title := "Plan actualizado"
	body := fmt.Sprintf("Tu plan pasó a %s. Ya podés usar los límites del nuevo plan.", strings.TrimSpace(planID))
	b.dispatch(ctx, companyID, models.UserNotificationKindPlanChanged, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

// FacturaReady after AFIP CAE is stored.
func (b *Broadcaster) FacturaReady(ctx context.Context, companyID uuid.UUID) {
	title := "Factura electrónica lista"
	body := "Emitimos la factura electrónica (AFIP) para tu último pago. Podés ver el detalle en Facturación."
	b.dispatch(ctx, companyID, models.UserNotificationKindFacturaReady, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

// FacturaFailed when automated emission stops retrying (or terminal validation error).
func (b *Broadcaster) FacturaFailed(ctx context.Context, companyID uuid.UUID, summary string) {
	title := "Factura electrónica pendiente"
	s := strings.TrimSpace(summary)
	if len(s) > 240 {
		s = s[:237] + "…"
	}
	body := "No pudimos emitir la factura electrónica automáticamente: " + s + " Contactá soporte si necesitás ayuda."
	b.dispatch(ctx, companyID, models.UserNotificationKindFacturaFailed, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

// DeviceRegistered when a new handset registers for the warehouse.
func (b *Broadcaster) DeviceRegistered(ctx context.Context, companyID uuid.UUID, warehouseLabel string) {
	title := "Nuevo dispositivo registrado"
	body := fmt.Sprintf("Se registró un dispositivo en %s.", strings.TrimSpace(warehouseLabel))
	if strings.TrimSpace(warehouseLabel) == "" {
		body = "Se registró un nuevo dispositivo en tu cuenta."
	}
	b.dispatch(ctx, companyID, models.UserNotificationKindDeviceRegistered, title, strPtr(body), b.panelURL("/panel/dispositivos"))
}

// DeviceRevoked when panel user revokes a device.
func (b *Broadcaster) DeviceRevoked(ctx context.Context, companyID uuid.UUID) {
	title := "Dispositivo revocado"
	body := "Un dispositivo pasó a estado revocado. El operario deja de sincronizar hasta que reactives el equipo."
	b.dispatch(ctx, companyID, models.UserNotificationKindDeviceRevoked, title, strPtr(body), b.panelURL("/panel/dispositivos"))
}

// DeviceReactivated when panel user reactivates a device.
func (b *Broadcaster) DeviceReactivated(ctx context.Context, companyID uuid.UUID) {
	title := "Dispositivo reactivado"
	body := "Un dispositivo volvió a estado activo y puede sincronizar de nuevo."
	b.dispatch(ctx, companyID, models.UserNotificationKindDeviceReactivated, title, strPtr(body), b.panelURL("/panel/dispositivos"))
}

// SessionTransferCompleted when desktop session is claimed on phone.
func (b *Broadcaster) SessionTransferCompleted(ctx context.Context, companyID uuid.UUID) {
	title := "Sesión vinculada"
	body := "Vinculamos tu sesión del panel en el teléfono. Cuando termines, cerrá sesión si no es tu equipo."
	b.dispatch(ctx, companyID, models.UserNotificationKindSessionTransferCompleted, title, strPtr(body), b.panelURL("/panel"))
}

// OperatorCreated when an admin creates an operator user.
func (b *Broadcaster) OperatorCreated(ctx context.Context, companyID uuid.UUID, username string) {
	title := "Nuevo operador"
	body := fmt.Sprintf("Se creó el operador %s. Compartí usuario y contraseña por un canal seguro.", strings.TrimSpace(username))
	b.dispatch(ctx, companyID, models.UserNotificationKindOperatorCreated, title, strPtr(body), b.panelURL("/panel/operadores"))
}

// PaymentMethodUpdated after Mercado Pago card on file changes.
func (b *Broadcaster) PaymentMethodUpdated(ctx context.Context, companyID uuid.UUID) {
	title := "Medio de pago actualizado"
	body := "Actualizamos la tarjeta guardada en Mercado Pago para futuros cobros."
	b.dispatch(ctx, companyID, models.UserNotificationKindPaymentMethodUpdated, title, strPtr(body), b.panelURL("/panel/facturacion"))
}

// DocumentsUsageWarning when MTD usage crosses 90% of plan limit (once per UTC month).
func (b *Broadcaster) DocumentsUsageWarning(ctx context.Context, companyID uuid.UUID, mtd, limit int64) {
	title := "Te acercás al límite de documentos"
	body := fmt.Sprintf("En lo que va del mes procesaste %d de %d documentos incluidos en tu plan.", mtd, limit)
	b.dispatch(ctx, companyID, models.UserNotificationKindDocumentsUsageWarning, title, strPtr(body), b.panelURL("/panel"))
}

// FirstScanCompleted once per company when the first inbound note exists (deduped).
func (b *Broadcaster) FirstScanCompleted(ctx context.Context, companyID uuid.UUID) {
	title := "Primer remito sincronizado"
	body := "Recibimos el primer remito en la nube. Seguí cargando desde la app para ver métricas en el panel."
	b.dispatch(ctx, companyID, models.UserNotificationKindFirstScanCompleted, title, strPtr(body), b.panelURL("/panel"))
}
