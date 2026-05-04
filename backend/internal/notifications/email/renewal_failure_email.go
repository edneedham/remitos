package email

import (
	"fmt"
	"strings"
)

const renewalFailureSubject = "No pudimos renovar tu suscripción automáticamente"

// RenewalChargeFailure notifies the owner that an automatic renewal charge failed (Spanish).
func RenewalChargeFailure(
	toEmail string,
	companyName string,
	amountMinor int64,
	currency string,
	reason string,
	publicSiteURL string,
) Message {
	name := strings.TrimSpace(companyName)
	if name == "" {
		name = "tu cuenta"
	}
	cur := strings.ToUpper(strings.TrimSpace(currency))
	if cur == "" {
		cur = "ARS"
	}
	whole := amountMinor / 100
	amountLine := fmt.Sprintf("%s $%s", cur, FormatARSWholeWithDots(whole))
	r := strings.TrimSpace(reason)
	if r == "" {
		r = "El cobro con la tarjeta guardada no fue aprobado."
	}

	var linkBlock strings.Builder
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base != "" {
		linkBlock.WriteString(fmt.Sprintf(
			`<p>Podés actualizar el medio de pago o revisar tu cuenta en <a href="%s/dashboard">%s/dashboard</a>.</p>`,
			base, base,
		))
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
<p>Hola,</p>
<p>Intentamos renovar la suscripción de <strong>%s</strong> en Remitos, pero <strong>el cobro no se completó</strong>.</p>
<p><strong>Importe intentado:</strong> %s</p>
<p>%s</p>
%s
<p>Si ya actualizaste el pago, podés ignorar este mensaje.</p>
<p>El equipo de Remitos</p>
</body></html>`,
		escapeHTML(name),
		escapeHTML(amountLine),
		escapeHTML(r),
		linkBlock.String(),
	)

	text := fmt.Sprintf(`Hola,

Intentamos renovar la suscripción de %s en Remitos, pero el cobro no se completó.

Importe intentado: %s

%s

%s
El equipo de Remitos
`, name, amountLine, r, textFailureLinks(publicSiteURL))

	return Message{
		To:       toEmail,
		Subject:  renewalFailureSubject,
		HTMLBody: html,
		TextBody: text,
	}
}

func textFailureLinks(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	return fmt.Sprintf("Gestionar cuenta: %s/dashboard\n", base)
}
