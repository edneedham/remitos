'use client';

import { useCallback, useMemo, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  DELIVERY_NOTES_PER_DAY_OPTIONS,
  PROCESSING_MODE_OPTIONS,
  WAITLIST_FIELD_MAX,
  type DeliveryNotesPerDayValue,
  type ProcessingModeValue,
} from '../lib/waitlistFormOptions';

function warehouseCountValid(raw: string): boolean {
  const t = raw.trim();
  if (t === '') return false;
  const n = Number.parseInt(t, 10);
  return Number.isFinite(n) && n >= 0 && n <= 50000;
}

function emailLooksValid(raw: string): boolean {
  const t = raw.trim();
  if (t.length < 5) return false;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(t);
}

function nonEmptyTrimmed(raw: string): boolean {
  return raw.trim().length > 0;
}

export default function WaitlistForm() {
  const searchParams = useSearchParams();
  const rawPlan = searchParams.get('plan');
  const planHint =
    rawPlan === 'pyme' || rawPlan === 'empresa' ? rawPlan : null;

  const [deliveryNotesPerDay, setDeliveryNotesPerDay] =
    useState<DeliveryNotesPerDayValue | ''>('');
  const [processingMode, setProcessingMode] = useState<ProcessingModeValue | ''>(
    '',
  );
  const [digitalApplication, setDigitalApplication] = useState('');
  const [warehouseCountInput, setWarehouseCountInput] = useState('');
  const [logisticsPainPoints, setLogisticsPainPoints] = useState('');

  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [doneMessage, setDoneMessage] = useState<string | null>(null);

  const canSubmit = useMemo(() => {
    if (loading) return false;
    return (
      Boolean(deliveryNotesPerDay) &&
      Boolean(processingMode) &&
      warehouseCountValid(warehouseCountInput) &&
      emailLooksValid(email) &&
      nonEmptyTrimmed(fullName) &&
      nonEmptyTrimmed(companyName) &&
      Boolean(getApiBaseUrl())
    );
  }, [
    deliveryNotesPerDay,
    processingMode,
    warehouseCountInput,
    email,
    fullName,
    companyName,
    loading,
  ]);

  const submit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      if (!canSubmit) {
        return;
      }
      setError(null);
      setDoneMessage(null);

      if (!deliveryNotesPerDay) {
        setError('Elegí el rango que mejor representa tus remitos por día.');
        return;
      }
      if (!processingMode) {
        setError('Elegí cómo procesás los remitos hoy.');
        return;
      }
      const whTrim = warehouseCountInput.trim();
      if (!warehouseCountValid(warehouseCountInput)) {
        setError(
          whTrim === ''
            ? 'Indicá cuántos depósitos tenés (podés poner 0).'
            : 'La cantidad de depósitos debe ser un número entre 0 y 50.000.',
        );
        return;
      }
      const wh = Number.parseInt(whTrim, 10);

      if (!emailLooksValid(email)) {
        setError('Ingresá un correo electrónico válido.');
        return;
      }
      if (!nonEmptyTrimmed(fullName)) {
        setError('Ingresá tu nombre.');
        return;
      }
      if (!nonEmptyTrimmed(companyName)) {
        setError('Ingresá el nombre de tu empresa u organización.');
        return;
      }

      const api = getApiBaseUrl();
      if (!api) {
        setError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        return;
      }

      setLoading(true);
      try {
        const sourceParts: string[] = ['website_waitlist'];
        if (planHint) sourceParts.push(`plan=${planHint}`);
        const res = await fetch(`${api}/public/waitlist`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: email.trim(),
            full_name: fullName.trim(),
            company_name: companyName.trim(),
            source: sourceParts.join(';'),
            delivery_notes_per_day_band: deliveryNotesPerDay,
            processing_mode: processingMode,
            digital_application:
              processingMode === 'digital' ? digitalApplication.trim() : '',
            logistics_pain_points: logisticsPainPoints.trim(),
            warehouse_count: wh,
          }),
        });
        const data = (await res.json().catch(() => ({}))) as {
          message?: string;
          error?: string;
          fields?: Record<string, string>;
          already_registered?: boolean;
        };
        if (!res.ok) {
          if (data.fields && typeof data.fields === 'object') {
            const first = Object.values(data.fields)[0];
            setError(
              typeof first === 'string'
                ? first
                : data.message || 'Revisá los datos e intentá de nuevo.',
            );
            return;
          }
          setError(
            data.message ||
              data.error ||
              'No pudimos guardar tu solicitud. Intentá de nuevo más tarde.',
          );
          return;
        }
        setDoneMessage(
          data.message ||
            'Gracias. Te sumamos a la lista de espera y te contactamos cuando haya acceso.',
        );
        if (!data.already_registered) {
          setDeliveryNotesPerDay('');
          setProcessingMode('');
          setDigitalApplication('');
          setWarehouseCountInput('');
          setLogisticsPainPoints('');
          setEmail('');
          setFullName('');
          setCompanyName('');
        }
      } catch {
        setError('Error de red. Revisá tu conexión e intentá de nuevo.');
      } finally {
        setLoading(false);
      }
    },
    [
      canSubmit,
      email,
      fullName,
      companyName,
      planHint,
      deliveryNotesPerDay,
      processingMode,
      digitalApplication,
      warehouseCountInput,
      logisticsPainPoints,
    ],
  );

  const radioClass =
    'flex cursor-pointer items-start gap-3 rounded-lg border border-gray-200 px-3 py-2.5 has-[:checked]:border-blue-500 has-[:checked]:bg-blue-50/60';

  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-6"
      noValidate
      aria-describedby={error ? 'waitlist-form-alert' : undefined}
    >
      {planHint === 'pyme' || planHint === 'empresa' ? (
        <p className="rounded-lg border border-blue-100 bg-blue-50/80 px-3 py-2 text-sm text-blue-900">
          Interés registrado para el plan{' '}
          <span className="font-semibold">
            {planHint === 'pyme' ? 'PyME' : 'Empresa'}
          </span>
          .
        </p>
      ) : null}

      <fieldset className="space-y-3">
        <legend className="mb-1 text-sm font-medium text-gray-900">
          ¿En qué rango está el promedio de remitos de entrega por día?{' '}
          <span className="text-red-600">*</span>
        </legend>
        <div className="flex flex-col gap-2">
          {DELIVERY_NOTES_PER_DAY_OPTIONS.map((opt) => (
            <label key={opt.value} className={radioClass}>
              <input
                type="radio"
                name="delivery_notes_per_day_band"
                value={opt.value}
                checked={deliveryNotesPerDay === opt.value}
                onChange={() => setDeliveryNotesPerDay(opt.value)}
                className="mt-1 h-4 w-4 shrink-0 text-blue-600"
              />
              <span className="text-sm text-gray-800">{opt.label}</span>
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset className="space-y-3">
        <legend className="mb-1 text-sm font-medium text-gray-900">
          ¿Cómo procesás los remitos hoy? <span className="text-red-600">*</span>
        </legend>
        <div className="flex flex-col gap-2">
          {PROCESSING_MODE_OPTIONS.map((opt) => (
            <label key={opt.value} className={radioClass}>
              <input
                type="radio"
                name="processing_mode"
                value={opt.value}
                checked={processingMode === opt.value}
                onChange={() => {
                  setProcessingMode(opt.value);
                  if (opt.value === 'manual') {
                    setDigitalApplication('');
                  }
                }}
                className="mt-1 h-4 w-4 shrink-0 text-blue-600"
              />
              <span className="text-sm text-gray-800">{opt.label}</span>
            </label>
          ))}
        </div>
        {processingMode === 'digital' ? (
          <div className="mt-2">
            <label
              htmlFor="waitlist-digital-app"
              className="mb-1.5 block text-sm font-medium text-gray-700"
            >
              ¿Qué aplicación usás? (opcional)
            </label>
            <input
              id="waitlist-digital-app"
              name="digital_application"
              type="text"
              autoComplete="off"
              maxLength={WAITLIST_FIELD_MAX.digitalApplication}
              value={digitalApplication}
              onChange={(e) => setDigitalApplication(e.target.value)}
              placeholder="Ej.: Excel, sistema ERP, otra…"
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
            />
          </div>
        ) : null}
      </fieldset>

      <div>
        <label
          htmlFor="waitlist-warehouses"
          className="mb-1.5 block text-sm font-medium text-gray-900"
        >
          ¿Cuántos depósitos tenés hoy? <span className="text-red-600">*</span>
        </label>
        <input
          id="waitlist-warehouses"
          name="warehouse_count"
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          autoComplete="off"
          maxLength={WAITLIST_FIELD_MAX.warehouseDigits}
          required
          value={warehouseCountInput}
          onChange={(e) => {
            const digitsOnly = e.target.value.replace(/\D/g, '');
            setWarehouseCountInput(
              digitsOnly.slice(0, WAITLIST_FIELD_MAX.warehouseDigits),
            );
          }}
          className="w-full max-w-[12rem] rounded-lg border border-gray-300 px-3 py-2.5 text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
        />
        <p className="mt-1 text-xs text-gray-500">
          Podés ingresar 0 si aún no tenés depósitos.
        </p>
      </div>

      <div>
        <label
          htmlFor="waitlist-logistics-pain"
          className="mb-1.5 block text-sm font-medium text-gray-900"
        >
          ¿Cuáles son hoy tus mayores problemas en la logística en Argentina? (opcional)
        </label>
        <textarea
          id="waitlist-logistics-pain"
          name="logistics_pain_points"
          rows={4}
          maxLength={WAITLIST_FIELD_MAX.logisticsPainPoints}
          autoComplete="off"
          value={logisticsPainPoints}
          onChange={(e) => setLogisticsPainPoints(e.target.value)}
          placeholder="Ej.: costos, demoras, visibilidad de stock, documentación, última milla…"
          className="w-full min-h-[5.5rem] resize-y rounded-lg border border-gray-300 px-3 py-2.5 text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
        />
        <p className="mt-1 text-xs text-gray-500">
          Hasta {WAITLIST_FIELD_MAX.logisticsPainPoints} caracteres.
        </p>
      </div>

      <div className="border-t border-gray-100 pt-2">
        <p className="mb-4 text-sm font-medium text-gray-700">
          Tus datos de contacto
        </p>
        <div className="flex flex-col gap-5">
          <div>
            <label
              htmlFor="waitlist-email"
              className="mb-1.5 block text-sm font-medium text-gray-700"
            >
              Correo electrónico <span className="text-red-600">*</span>
            </label>
            <input
              id="waitlist-email"
              name="email"
              type="email"
              autoComplete="email"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              maxLength={WAITLIST_FIELD_MAX.email}
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
            />
          </div>

          <div>
            <label
              htmlFor="waitlist-name"
              className="mb-1.5 block text-sm font-medium text-gray-700"
            >
              Nombre <span className="text-red-600">*</span>
            </label>
            <input
              id="waitlist-name"
              name="full_name"
              type="text"
              autoComplete="name"
              autoCapitalize="words"
              maxLength={WAITLIST_FIELD_MAX.fullName}
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
            />
          </div>

          <div>
            <label
              htmlFor="waitlist-company"
              className="mb-1.5 block text-sm font-medium text-gray-700"
            >
              Empresa <span className="text-red-600">*</span>
            </label>
            <input
              id="waitlist-company"
              name="company_name"
              type="text"
              autoComplete="organization"
              maxLength={WAITLIST_FIELD_MAX.companyName}
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30"
            />
          </div>
        </div>
      </div>

      {error ? (
        <p
          id="waitlist-form-alert"
          role="alert"
          className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {error}
        </p>
      ) : null}

      {doneMessage ? (
        <p
          role="status"
          className="rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-900"
        >
          {doneMessage}
        </p>
      ) : null}

      {!canSubmit && !loading && !doneMessage ? (
        <p className="text-center text-xs text-gray-500">
          {getApiBaseUrl()
            ? 'Completá las preguntas obligatorias, el rango de remitos por día, la cantidad de depósitos, nombre, empresa y tu correo para enviar.'
            : 'Falta configurar NEXT_PUBLIC_API_URL: el envío no está disponible hasta que esté la URL de la API.'}
        </p>
      ) : null}

      <button
        type="submit"
        disabled={!canSubmit || loading}
        title={
          loading
            ? undefined
            : !getApiBaseUrl()
              ? 'Falta configurar NEXT_PUBLIC_API_URL'
              : !canSubmit
                ? 'Completá todos los campos obligatorios'
                : undefined
        }
        className="inline-flex w-full items-center justify-center rounded-lg bg-blue-600 px-4 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {loading ? 'Enviando…' : 'Unirme a la lista'}
      </button>
    </form>
  );
}
