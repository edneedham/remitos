package email

import (
	"fmt"
	"strings"
	"time"
)

const trialEndNoticeSubject = "Tu prueba en Remitos termina pronto"

// TrialEndingSoon sends when trial calendar day is 3 days away (Argentina).
func TrialEndingSoon(toEmail, companyName, trialEndsPhrase, publicSiteURL string) Message {
	name := strings.TrimSpace(companyName)
	if name == "" {
		name = "tu cuenta"
	}
	var link strings.Builder
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base != "" {
		link.WriteString(fmt.Sprintf(`<p><a href="%s/dashboard">Elegir plan y activar suscripción</a></p>`, base))
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
<p>Hola,</p>
<p>La prueba gratuita de <strong>%s</strong> en Remitos <strong>termina en 3 días</strong> (%s).</p>
<p>Para no perder el acceso, activá un plan con facturación en tu cuenta.</p>
%s
<p>Saludos,<br/>El equipo de Remitos</p>
</body></html>`,
		escapeHTML(name),
		escapeHTML(trialEndsPhrase),
		link.String(),
	)

	text := fmt.Sprintf(`Hola,

La prueba gratuita de %s en Remitos termina en 3 días (%s).

Para no perder el acceso, activá un plan con facturación en tu cuenta.

%s
Saludos,
El equipo de Remitos
`, name, trialEndsPhrase, textTrialEndLinks(publicSiteURL))

	return Message{To: toEmail, Subject: trialEndNoticeSubject, HTMLBody: html, TextBody: text}
}

func textTrialEndLinks(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	return fmt.Sprintf("Tu cuenta: %s/dashboard\n", base)
}

// FormatTrialEndDateEs returns a long date in Spanish for t in the given location.
func FormatTrialEndDateEs(t time.Time, loc *time.Location) string {
	if loc == nil {
		loc = time.UTC
	}
	lt := t.In(loc)
	return formatDateEs(lt)
}
