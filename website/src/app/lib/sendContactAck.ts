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

  const wordmark = htmlWordmarkBlock();

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
      html: `${wordmark}<p>Hola ${escapeHtml(safeName)},</p><p>Gracias por contactarnos. Te responderemos pronto.</p><p>— Equipo Remitos</p>`,
      text: `Hola ${safeName},\n\nGracias por contactarnos. Te responderemos pronto.\n\n— Equipo Remitos`,
    }),
  });

  if (!res.ok) {
    const body = await res.text().catch(() => '');
    console.warn('[contact] Resend ack failed:', res.status, body);
  }
}

/** Same asset path as backend `WordmarkPNGPath` (`website/public/enpunto-wordmark.png`). */
function htmlWordmarkBlock(): string {
  const base = (process.env.NEXT_PUBLIC_SITE_URL ?? '').trim().replace(/\/$/, '');
  if (!base) {
    return '';
  }
  const src = escapeHtml(`${base}/enpunto-wordmark.png`);
  return `<p style="margin:0 0 20px 0;line-height:0;"><img src="${src}" alt="En Punto" width="180" style="display:block;border:0;outline:none;text-decoration:none;max-width:180px;height:auto;"></p>`;
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
