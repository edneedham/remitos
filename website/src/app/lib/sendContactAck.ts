/**
 * Optional branded acknowledgement via Resend after Formspree accepts the message.
 * Set RESEND_API_KEY and CONTACT_ACK_EMAIL_FROM (e.g. Remitos <billing@remitos.com>).
 */
export async function sendContactAckEmail(to: string, name: string): Promise<void> {
  const apiKey = process.env.RESEND_API_KEY;
  const from = process.env.CONTACT_ACK_EMAIL_FROM;
  if (!apiKey?.trim() || !from?.trim()) {
    return;
  }
  const safeName = name.trim() || 'hola';
  const safeTo = to.trim();
  if (!safeTo) return;

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      from: from.trim(),
      to: [safeTo],
      subject: 'Recibimos tu mensaje — Remitos',
      html: `<p>Hola ${escapeHtml(safeName)},</p><p>Gracias por contactarnos. Te responderemos pronto.</p><p>— Equipo Remitos</p>`,
      text: `Hola ${safeName},\n\nGracias por contactarnos. Te responderemos pronto.\n\n— Equipo Remitos`,
    }),
  });

  if (!res.ok) {
    const body = await res.text().catch(() => '');
    console.warn('[contact] Resend ack failed:', res.status, body);
  }
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
