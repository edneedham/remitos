package email

import (
	"fmt"
	"strings"
	"time"
)

const signupTrialWelcomeSubject = "Tu prueba en Remitos"

// SignupTrialWelcome builds the post-signup trial welcome message (Spanish copy).
func SignupTrialWelcome(toEmail, companyCode, companyName string, trialEndsAt time.Time, publicSiteURL string) Message {
	code := strings.TrimSpace(companyCode)
	name := strings.TrimSpace(companyName)
	if name == "" {
		name = code
	}
	end := trialEndsAt.UTC().Format(time.RFC3339)

	var linkBlock strings.Builder
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base != "" {
		linkBlock.WriteString(fmt.Sprintf(`
<p><strong>Enlaces útiles:</strong></p>
<ul>
  <li><a href="%s/account">Tu cuenta</a></li>
  <li><a href="%s/download">Descargar la app</a></li>
</ul>
`, base, base))
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
<p>Hola,</p>
<p>Tu cuenta de prueba para <strong>%s</strong> ya está lista.</p>
<p><strong>Código de empresa:</strong> %s<br/>
<strong>Fin de la prueba:</strong> %s (UTC)</p>
%s
<p>Podés iniciar sesión con el correo y la contraseña que usaste al registrarte.</p>
<p>Saludos,<br/>El equipo de Remitos</p>
</body></html>`, escapeHTML(name), escapeHTML(code), escapeHTML(end), linkBlock.String())

	text := fmt.Sprintf(`Hola,

Tu cuenta de prueba para %s ya está lista.

Código de empresa: %s
Fin de la prueba (UTC): %s

%s

Podés iniciar sesión con el correo y la contraseña que usaste al registrarte.

Saludos,
El equipo de Remitos
`, name, code, end, textLinks(publicSiteURL))

	return Message{
		To:       toEmail,
		Subject:  signupTrialWelcomeSubject,
		HTMLBody: html,
		TextBody: text,
	}
}

func textLinks(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	return fmt.Sprintf("Enlaces útiles:\n- Cuenta: %s/account\n- Descarga: %s/download\n", base, base)
}

func escapeHTML(s string) string {
	s = strings.ReplaceAll(s, "&", "&amp;")
	s = strings.ReplaceAll(s, "<", "&lt;")
	s = strings.ReplaceAll(s, ">", "&gt;")
	s = strings.ReplaceAll(s, `"`, "&quot;")
	return s
}
