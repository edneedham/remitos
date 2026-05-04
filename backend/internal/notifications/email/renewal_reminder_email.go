package email

import (
	"fmt"
	"math"
	"strconv"
	"strings"
	"time"
)

const subscriptionRenewalUpcomingSubject = "Recordatorio: próximo cobro de tu suscripción"

var monthsEs = []string{
	"enero", "febrero", "marzo", "abril", "mayo", "junio",
	"julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
}

// SubscriptionRenewalUpcoming builds the 3-day renewal heads-up (USD list + estimated ARS from current MEP buffer).
func SubscriptionRenewalUpcoming(
	toEmail string,
	companyName string,
	usdMajor float64,
	estimatedARSWhole int64,
	subscriptionExpiresAt time.Time,
	publicSiteURL string,
) Message {
	name := strings.TrimSpace(companyName)
	if name == "" {
		name = "tu empresa"
	}

	loc, err := time.LoadLocation("America/Argentina/Buenos_Aires")
	if err != nil {
		loc = time.UTC
	}
	localEnd := subscriptionExpiresAt.In(loc)
	billingDatePhrase := formatDateEs(localEnd)

	usdStr := formatUSDList(usdMajor)
	arsStr := FormatARSWholeWithDots(estimatedARSWhole)

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
<p>Hola,</p>
<p>Tu suscripción de <strong>%s</strong> en Remitos se renovará <strong>en 3 días</strong>.</p>
<p><strong>Fecha de renovación:</strong> %s (Argentina)</p>
<p><strong>Monto estimado:</strong><br/>
$%s USD (~ ARS %s)</p>
<p><em>El monto final puede variar según el tipo de cambio del día.</em></p>
%s
<p>Saludos,<br/>El equipo de Remitos</p>
</body></html>`,
		escapeHTML(name),
		escapeHTML(billingDatePhrase),
		escapeHTML(usdStr),
		escapeHTML(arsStr),
		renewalReminderLinkBlock(publicSiteURL),
	)

	text := fmt.Sprintf(`Hola,

Tu suscripción de %s en Remitos se renovará en 3 días.

Fecha de renovación: %s (Argentina)

Monto estimado:
$%s USD (~ ARS %s)

El monto final puede variar según el tipo de cambio del día.

%s
Saludos,
El equipo de Remitos
`, name, billingDatePhrase, usdStr, arsStr, renewalReminderTextLinks(publicSiteURL))

	return Message{
		To:       toEmail,
		Subject:  subscriptionRenewalUpcomingSubject,
		HTMLBody: html,
		TextBody: text,
	}
}

func formatDateEs(t time.Time) string {
	m := int(t.Month())
	if m < 1 || m > 12 {
		m = 1
	}
	return fmt.Sprintf("%d de %s de %d", t.Day(), monthsEs[m-1], t.Year())
}

func formatUSDList(usd float64) string {
	if usd <= 0 {
		return "0"
	}
	if math.Abs(usd-math.Round(usd)) < 0.001 {
		return strconv.FormatInt(int64(math.Round(usd)), 10)
	}
	return strings.TrimRight(strings.TrimRight(fmt.Sprintf("%.2f", usd), "0"), ".")
}

func renewalReminderLinkBlock(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	return fmt.Sprintf(`<p><a href="%s/dashboard">Gestionar suscripción y facturación</a></p>`, base)
}

func renewalReminderTextLinks(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	return fmt.Sprintf("Gestionar suscripción: %s/dashboard\n", base)
}
