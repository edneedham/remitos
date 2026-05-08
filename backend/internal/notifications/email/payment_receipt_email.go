package email

import (
	"fmt"
	"strings"
	"time"
)

const paymentReceiptSubject = "Comprobante de pago — Remitos"

// PaymentReceipt builds the paid invoice receipt (Spanish).
func PaymentReceipt(
	toEmail, companyName, planLabel, publicSiteURL string,
	amountMinor int64,
	currency string,
	paidAt time.Time,
	invoiceID string,
	mpPaymentID string,
	legalFooterAR string,
	afipFacturaNote string,
) Message {
	name := strings.TrimSpace(companyName)
	if name == "" {
		name = "tu cuenta"
	}
	plan := strings.TrimSpace(planLabel)
	if plan == "" {
		plan = "Suscripción"
	}
	whole := amountMinor / 100
	cur := strings.ToUpper(strings.TrimSpace(currency))
	if cur == "" {
		cur = "ARS"
	}
	amountLine := fmt.Sprintf("%s $%s", cur, FormatARSWholeWithDots(whole))

	loc, err := time.LoadLocation("America/Argentina/Buenos_Aires")
	if err != nil {
		loc = time.UTC
	}
	when := paidAt.In(loc).Format("02/01/2006 15:04") + " (Argentina)"
	legal := strings.TrimSpace(legalFooterAR)
	if legal == "" {
		legal = "Los importes se liquidan en pesos argentinos según el tipo de cambio aplicable a la fecha de pago."
	}

	var linkBlock strings.Builder
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base != "" {
		linkBlock.WriteString(fmt.Sprintf(`<p><a href="%s/dashboard">Ver facturación</a></p>`, base))
	}

	afipBlock := ""
	if s := strings.TrimSpace(afipFacturaNote); s != "" {
		afipBlock = fmt.Sprintf(`<p><strong>AFIP / ARCA:</strong> %s</p>`, escapeHTML(s))
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
<p>Hola,</p>
<p>Registramos un pago para <strong>%s</strong> en Remitos.</p>
<p><strong>Importe:</strong> %s<br/>
<strong>Plan:</strong> %s<br/>
<strong>Fecha:</strong> %s</p>
<p><strong>Comprobante interno:</strong> %s<br/>
<strong>ID pago Mercado Pago:</strong> %s</p>
%s
<p><em>%s</em></p>
%s
<p>Gracias por confiar en Remitos.</p>
</body></html>`,
		escapeHTML(name),
		escapeHTML(amountLine),
		escapeHTML(plan),
		escapeHTML(when),
		escapeHTML(invoiceID),
		escapeHTML(mpPaymentID),
		afipBlock,
		escapeHTML(legal),
		linkBlock.String(),
	)

	afipText := ""
	if s := strings.TrimSpace(afipFacturaNote); s != "" {
		afipText = "AFIP / ARCA: " + s + "\n\n"
	}
	text := fmt.Sprintf(`Hola,

Registramos un pago para %s en Remitos.

Importe: %s
Plan: %s
Fecha: %s

Comprobante interno: %s
ID pago Mercado Pago: %s

%s%s

%s
Gracias por confiar en Remitos.
`, name, amountLine, plan, when, invoiceID, mpPaymentID, afipText, legal, textBillingLinks(publicSiteURL))

	return Message{
		To:       toEmail,
		Subject:  paymentReceiptSubject,
		HTMLBody: html,
		TextBody: text,
	}
}

func textBillingLinks(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	return fmt.Sprintf("Facturación: %s/dashboard\n", base)
}
