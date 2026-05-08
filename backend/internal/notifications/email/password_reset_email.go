package email

import (
	"fmt"
	"strings"
)

const passwordResetSubject = "Restablecé tu contraseña — Remitos"

// PasswordReset builds the reset link email (Spanish).
func PasswordReset(toEmail, resetURL, publicSiteURL string) Message {
	to := strings.TrimSpace(toEmail)
	url := strings.TrimSpace(resetURL)
	if url == "" {
		url = "—"
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html><body>
%s
<p>Hola,</p>
<p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en Remitos.</p>
<p><a href="%s">Hacé clic acá para elegir una contraseña nueva</a></p>
<p>Si el enlace no funciona, copiá y pegá esta dirección en el navegador:</p>
<p style="word-break:break-all;">%s</p>
<p>Si no pediste este correo, podés ignorarlo.</p>
<p>El enlace vence en una hora.</p>
<p>Saludos,<br/>El equipo de Remitos</p>
</body></html>`, HTMLWordmarkBlock(publicSiteURL), escapeHTML(url), escapeHTML(url))

	text := fmt.Sprintf(`Hola,

Recibimos una solicitud para restablecer la contraseña de tu cuenta en Remitos.

Abrí este enlace en el navegador para elegir una contraseña nueva:
%s

Si no pediste este correo, podés ignorarlo. El enlace vence en una hora.

Saludos,
El equipo de Remitos
`, url)

	return Message{
		To:       to,
		Subject:  passwordResetSubject,
		HTMLBody: html,
		TextBody: text,
	}
}
