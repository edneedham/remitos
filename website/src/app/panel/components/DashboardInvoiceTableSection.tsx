'use client';

import { Download } from 'lucide-react';
import type { BillingInvoiceRow } from '../lib/entitlementTypes';
import {
  formatInvoiceDate,
  formatInvoiceMoney,
  invoiceStatusLabel,
} from '../lib/invoiceFormat';

type Props = {
  invoicesError: string | null;
  invoices: BillingInvoiceRow[];
  onDownloadTextInvoice: (invoice: BillingInvoiceRow) => void;
};

export default function DashboardInvoiceTableSection({
  invoicesError,
  invoices,
  onDownloadTextInvoice,
}: Props) {
  return (
    <section
      className="hidden md:block rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
      aria-labelledby="invoices-heading"
    >
      <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
        <h2 id="invoices-heading" className="text-base font-semibold text-gray-900">
          Facturas
        </h2>
        <p className="text-xs text-gray-500 sm:text-sm">
          Descargá el detalle de cada comprobante.
        </p>
      </div>

      {invoicesError ? (
        <p
          className="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
          role="alert"
        >
          {invoicesError}
        </p>
      ) : null}

      {!invoicesError && invoices.length === 0 ? (
        <p className="mt-4 rounded-lg border border-dashed border-gray-200 bg-gray-50 px-4 py-6 text-center text-sm text-gray-600">
          Todavía no hay facturas para mostrar.
        </p>
      ) : null}

      {!invoicesError && invoices.length > 0 ? (
        <div className="mt-4 overflow-x-auto rounded-lg border border-gray-200">
          <table className="w-full min-w-[52rem] text-left text-sm">
            <thead className="border-b border-gray-200 bg-gray-50">
              <tr>
                <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                  Fecha
                </th>
                <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                  Importe
                </th>
                <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                  Estado
                </th>
                <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                  Concepto
                </th>
                <th scope="col" className="px-4 py-3 text-right font-semibold text-gray-700">
                  Descargar
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {invoices.map((inv) => (
                <tr key={inv.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-gray-900">
                    {formatInvoiceDate(inv.issued_at)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 tabular-nums text-gray-900">
                    {formatInvoiceMoney(inv.amount_minor, inv.currency)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-gray-900">
                    {invoiceStatusLabel(inv.status)}
                  </td>
                  <td className="max-w-[20rem] px-4 py-3 text-gray-700">
                    {inv.description?.trim() ? inv.description : '—'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right">
                    <button
                      type="button"
                      onClick={() => onDownloadTextInvoice(inv)}
                      className="inline-flex items-center justify-center gap-2 rounded-lg border border-blue-200 bg-white px-3 py-2 text-xs font-semibold text-blue-700 hover:bg-blue-50"
                      aria-label={`Descargar factura ${inv.id}`}
                    >
                      <Download className="h-4 w-4 text-blue-600" aria-hidden />
                      Descargar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}
