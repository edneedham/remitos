package email

import (
	"fmt"
	"strings"
)

const (
	onboardingNudgeSetupSubject = "Seguí con la app En Punto (Android)"
	onboardingNudgeDay1Subject  = "¿Ya instalaste En Punto?"
	onboardingNudgeDay3Subject  = "¿Necesitás ayuda con tu primer remito?"
)

// TrialOnboardingNudgeSetup is the +10 minute reminder (download / trial-started links).
func TrialOnboardingNudgeSetup(toEmail, companyCode, companyName, publicSiteURL string) Message {
	return onboardingNudgeMessage(toEmail, companyCode, companyName, publicSiteURL, onboardingNudgeSetupSubject, `
<p>Estamos para ayudarte a terminar la configuración:</p>
<ul>
  <li>Descargá la app <strong>Android</strong> (APK) desde el panel.</li>
  <li>Iniciá sesión en el teléfono con el mismo usuario que en la web.</li>
  <li>Escaneá tu primer remito para ver datos en el panel.</li>
</ul>
`)
}

// TrialOnboardingNudgeDay1 is the +24 hour reminder.
func TrialOnboardingNudgeDay1(toEmail, companyCode, companyName, publicSiteURL string) Message {
	return onboardingNudgeMessage(toEmail, companyCode, companyName, publicSiteURL, onboardingNudgeDay1Subject, `
<p>Si todavía no procesaste un remito en la app, abrí En Punto en tu teléfono e iniciá sesión.</p>
<p>La app de depósito es solo <strong>Android</strong>; desde la web podés copiar el enlace de descarga o pasarlo al teléfono con el código QR.</p>
`)
}

// TrialOnboardingNudgeDay3 is the +72 hour support-oriented reminder.
func TrialOnboardingNudgeDay3(toEmail, companyCode, companyName, publicSiteURL string) Message {
	return onboardingNudgeMessage(toEmail, companyCode, companyName, publicSiteURL, onboardingNudgeDay3Subject, `
<p>Si algo te frenó (instalación, primer login o escaneo), contestá este correo y te ayudamos.</p>
<p>Recordá: codigo de empresa, mismo usuario y contraseña que en el sitio, y primer escaneo desde la app Android.</p>
`)
}

func onboardingNudgeMessage(toEmail, companyCode, companyName, publicSiteURL, subject, bodyHTML string) Message {
	code := strings.TrimSpace(companyCode)
	name := strings.TrimSpace(companyName)
	if name == "" {
		name = code
	}
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")

	var links strings.Builder
	if base != "" {
		links.WriteString(fmt.Sprintf(`
<p><strong>Enlaces útiles:</strong></p>
<ul>
  <li><a href="%s/trial-started">Guía de inicio</a></li>
  <li><a href="%s/dashboard/app">Descargar la app (Android)</a></li>
  <li><a href="%s/dashboard">Panel web</a></li>
</ul>
`, base, base, base))
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
<p>Hola,</p>
<p><strong>%s</strong> · código <strong>%s</strong></p>
%s
%s
<p>Saludos,<br/>El equipo de En Punto</p>
</body></html>`,
		escapeHTML(name),
		escapeHTML(code),
		bodyHTML,
		links.String())

	text := fmt.Sprintf(`Hola,

%s · código %s

%s

%s

Saludos,
El equipo de En Punto
`, name, code, strings.TrimSpace(stripHTMLHints(bodyHTML)), textLinksOnboarding(base))

	return Message{
		To:       toEmail,
		Subject:  subject,
		HTMLBody: html,
		TextBody: text,
	}
}

func stripHTMLHints(s string) string {
	s = strings.ReplaceAll(s, "<p>", "")
	s = strings.ReplaceAll(s, "</p>", "\n\n")
	s = strings.ReplaceAll(s, "<ul>", "")
	s = strings.ReplaceAll(s, "</ul>", "")
	s = strings.ReplaceAll(s, "<li>", "- ")
	s = strings.ReplaceAll(s, "</li>", "\n")
	s = strings.ReplaceAll(s, "<strong>", "")
	s = strings.ReplaceAll(s, "</strong>", "")
	return strings.TrimSpace(s)
}

func textLinksOnboarding(base string) string {
	if base == "" {
		return ""
	}
	return fmt.Sprintf(`Enlaces:
- Guía: %s/trial-started
- App Android: %s/dashboard/app
- Panel: %s/dashboard
`, base, base, base)
}
