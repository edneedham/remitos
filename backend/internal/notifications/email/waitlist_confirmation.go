package email

import (
	"fmt"
	"strings"
)

const waitlistJoinedSubject = "Te sumamos a la lista de espera — Remitos"

// WaitlistJoined confirms the address was recorded (Spanish copy).
func WaitlistJoined(toEmail, publicSiteURL string) Message {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	var linkPara strings.Builder
	if base != "" {
		linkPara.WriteString(fmt.Sprintf(`<p>Cuando abramos el registro, vas a poder entrar desde <a href="%s">%s</a>.</p>`, base, escapeHTML(base)))
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
%s
<p>Hola,</p>
<p>Recibimos tu correo y te sumamos a la lista de espera de Remitos.</p>
%s
<p>Te vamos a avisar cuando podamos darte acceso.</p>
<p>Saludos,<br/>El equipo de Remitos</p>
</body></html>`, HTMLWordmarkBlock(publicSiteURL), linkPara.String())

	text := fmt.Sprintf(`Hola,

Recibimos tu correo y te sumamos a la lista de espera de Remitos.

Te vamos a avisar cuando podamos darte acceso.

Saludos,
El equipo de Remitos
`)
	if base != "" {
		text += fmt.Sprintf("\nSitio: %s\n", base)
	}

	return Message{
		To:       toEmail,
		Subject:  waitlistJoinedSubject,
		HTMLBody: html,
		TextBody: text,
	}
}
