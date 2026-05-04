package email

import (
	"fmt"
	"strings"
	"time"
)

const subscriptionLapseSubject = "Tu suscripción en Remitos venció"

// SubscriptionLapsed notifies that the paid period ended and access is affected.
func SubscriptionLapsed(toEmail, companyName, expiredOnPhrase, publicSiteURL string) Message {
	name := strings.TrimSpace(companyName)
	if name == "" {
		name = "tu cuenta"
	}
	var link strings.Builder
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base != "" {
		link.WriteString(fmt.Sprintf(`<p><a href="%s/dashboard">Renovar o actualizar pago</a></p>`, base))
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
<p>Hola,</p>
<p>La suscripción de <strong>%s</strong> alcanzó su fecha de vencimiento <strong>(%s)</strong>.</p>
<p>Si el pago no se acreditó, es posible que el acceso a funciones de la app o la web esté restringido hasta que regularices la facturación.</p>
%s
<p>¿Necesitás ayuda? Respondé a este correo o escribinos desde el sitio.</p>
<p>El equipo de Remitos</p>
</body></html>`,
		escapeHTML(name),
		escapeHTML(expiredOnPhrase),
		link.String(),
	)

	text := fmt.Sprintf(`Hola,

La suscripción de %s alcanzó su fecha de vencimiento (%s).

Si el pago no se acreditó, el acceso puede estar restringido hasta regularizar la facturación.

%s
El equipo de Remitos
`, name, expiredOnPhrase, textLapseLinks(publicSiteURL))

	return Message{To: toEmail, Subject: subscriptionLapseSubject, HTMLBody: html, TextBody: text}
}

func textLapseLinks(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	return fmt.Sprintf("Gestionar cuenta: %s/dashboard\n", base)
}

// FormatLapseDateEs formats the subscription end instant for copy (Argentina).
func FormatLapseDateEs(t time.Time, loc *time.Location) string {
	if loc == nil {
		loc = time.UTC
	}
	return formatDateEs(t.In(loc))
}
