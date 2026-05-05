export function formatInvoiceMoney(amountMinor: number, currency: string): string {
  const major = amountMinor / 100;
  try {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: currency || 'ARS',
      minimumFractionDigits: 2,
    }).format(major);
  } catch {
    return `${major.toFixed(2)} ${currency}`;
  }
}

export function invoiceStatusLabel(status: string): string {
  switch (status.toLowerCase().trim()) {
    case 'paid':
      return 'Pagado';
    case 'pending':
      return 'Pendiente';
    case 'void':
      return 'Anulado';
    default:
      return status;
  }
}

export function formatInvoiceDate(iso: string): string {
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) return '—';
  return parsed.toLocaleDateString('es-AR', {
    dateStyle: 'long',
  });
}
