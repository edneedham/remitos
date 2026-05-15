'use client';

import {
  Gift,
  ListOrdered,
  MessageSquare,
  Package,
  Quote,
  UserRound,
} from 'lucide-react';
import Link from 'next/link';
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
  const [productUpdatesOptIn, setProductUpdatesOptIn] = useState(false);
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
            product_updates_opt_in: productUpdatesOptIn,
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
          setProductUpdatesOptIn(false);
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
      productUpdatesOptIn,
    ],
  );

  const checkboxOptionClass =
    'flex cursor-pointer items-start gap-3 rounded-lg border border-gray-200 px-3 py-3 has-[:checked]:border-blue-500 has-[:checked]:bg-blue-50/60';

  const radioClass =
    'flex cursor-pointer items-start gap-3 rounded-lg border border-gray-200 px-3 py-3 has-[:checked]:border-blue-500 has-[:checked]:bg-blue-50/60';
  const iconTileClass =
    'flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600';
  const eyebrowClass =
    'text-xs font-semibold uppercase tracking-[0.2em] text-blue-600';
  const sectionTitleClass = 'text-base font-semibold text-gray-900';
  const sectionDescClass = 'mt-1 text-sm leading-relaxed text-gray-600';
  const questionLabelClass =
    'block text-sm font-semibold leading-snug text-gray-900';
  const legendClass = `${questionLabelClass} mb-2`;
  const fieldLabelClass = `${questionLabelClass} mb-1.5`;
  const optionTextClass = 'text-sm leading-relaxed text-gray-700';
  const finePrintClass = 'text-xs leading-relaxed text-gray-500';
  const inputClass =
    'w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30';
  const fieldHintClass = `mt-1.5 ${finePrintClass}`;

  return (
    <form
      onSubmit={submit}
      className="flex flex-col"
      noValidate
      aria-describedby={error ? 'waitlist-form-alert' : undefined}
    >
      <div className="flex flex-col gap-8 md:grid md:grid-cols-[minmax(0,2fr)_minmax(0,3fr)] md:items-stretch md:gap-10 lg:gap-12">
        <div className="md:border-r md:border-gray-200 md:pr-10 lg:pr-12">
          <header className="md:sticky md:top-6">
            <div className="space-y-2.5">
              <p className={eyebrowClass}>Acceso anticipado</p>
              <h1 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
                Unite a la lista de espera
              </h1>
              <p className={`max-w-sm ${sectionDescClass} mt-0`}>
                Enterate primero cuando abramos las suscripciones.
              </p>
            </div>

            <ul className="mt-8 space-y-4 border-t border-gray-100 pt-8">
              <li className="flex items-start gap-3">
                <span
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600"
                  aria-hidden
                >
                  <ListOrdered className="h-4 w-4" />
                </span>
                <span className="min-w-0 pt-0.5">
                  <span className={`block ${sectionTitleClass}`}>
                    Primero en la fila
                  </span>
                  <span className={`block ${sectionDescClass}`}>
                    Obtené acceso anticipado antes de abrir al público.
                  </span>
                </span>
              </li>
              <li className="flex items-start gap-3">
                <span
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600"
                  aria-hidden
                >
                  <Gift className="h-4 w-4" />
                </span>
                <span className="min-w-0 pt-0.5">
                  <span className={`block ${sectionTitleClass}`}>
                    Beneficios para miembros fundadores
                  </span>
                  <span className={`block ${sectionDescClass}`}>
                    Descuentos especiales y ventajas para nuestros primeros
                    clientes.
                  </span>
                </span>
              </li>
              <li className="flex items-start gap-3">
                <span
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600"
                  aria-hidden
                >
                  <MessageSquare className="h-4 w-4" />
                </span>
                <span className="min-w-0 pt-0.5">
                  <span className={`block ${sectionTitleClass}`}>
                    Ayudanos a dar forma al producto
                  </span>
                  <span className={`block ${sectionDescClass}`}>
                    Tu feedback nos ayudará a construir lo que realmente
                    necesitás.
                  </span>
                </span>
              </li>
            </ul>

            <blockquote className="mt-8 rounded-xl border border-gray-100 bg-gray-50/90 p-4 sm:p-5">
              <div className="flex items-start gap-3">
                <span
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-100/80 text-blue-600"
                  aria-hidden
                >
                  <Quote className="h-4 w-4" />
                </span>
                <p className="min-w-0">
                  <span className={`block ${sectionDescClass} mt-0`}>
                    Lo estamos construyendo para equipos como el tuyo. Contanos
                    un poco sobre vos para que podamos hacerlo aún mejor.
                  </span>
                  <span className={`mt-3 block ${questionLabelClass}`}>
                    — El equipo
                  </span>
                </p>
              </div>
            </blockquote>
          </header>
        </div>

        <div className="flex min-w-0 flex-col">
          {planHint === 'pyme' || planHint === 'empresa' ? (
            <p className="mb-6 rounded-lg border border-blue-100 bg-blue-50/80 px-3 py-2.5 text-sm text-blue-900">
              Interés registrado para el plan{' '}
              <span className="font-semibold">
                {planHint === 'pyme' ? 'PyME' : 'Empresa'}
              </span>
              .
            </p>
          ) : null}

          <section className="space-y-5">
            <div className="flex items-start gap-3">
              <span className={iconTileClass} aria-hidden>
                <UserRound className="h-4 w-4" />
              </span>
              <div>
                <h2 className={sectionTitleClass}>Tus datos de contacto</h2>
                <p className={sectionDescClass}>
                  Los usamos para avisarte cuando haya acceso.
                </p>
              </div>
            </div>
            <div className="flex flex-col gap-5">
              <div>
                <label htmlFor="waitlist-email" className={fieldLabelClass}>
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
                  className={inputClass}
                />
              </div>

              <div>
                <label htmlFor="waitlist-name" className={fieldLabelClass}>
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
                  className={inputClass}
                />
              </div>

              <div>
                <label htmlFor="waitlist-company" className={fieldLabelClass}>
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
                  className={inputClass}
                />
              </div>
            </div>
          </section>

          <section className="mt-8 space-y-6 border-t border-gray-100 pt-8">
            <div className="flex items-start gap-3">
              <span className={iconTileClass} aria-hidden>
                <Package className="h-4 w-4" />
              </span>
              <div>
                <h2 className={sectionTitleClass}>Sobre tu operación</h2>
                <p className={sectionDescClass}>
                  Estas respuestas nos ayudan a entender tu volumen y tus
                  prioridades.
                </p>
              </div>
            </div>

            <fieldset className="space-y-3">
              <legend className={legendClass}>
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
                    <span className={optionTextClass}>{opt.label}</span>
                  </label>
                ))}
              </div>
            </fieldset>

            <fieldset className="space-y-3">
              <legend className={legendClass}>
                ¿Cómo procesás los remitos hoy?{' '}
                <span className="text-red-600">*</span>
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
                    <span className={optionTextClass}>{opt.label}</span>
                  </label>
                ))}
              </div>
              {processingMode === 'digital' ? (
                <div className="mt-3">
                  <label
                    htmlFor="waitlist-digital-app"
                    className={fieldLabelClass}
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
                    className={inputClass}
                  />
                </div>
              ) : null}
            </fieldset>

            <div>
              <label htmlFor="waitlist-warehouses" className={fieldLabelClass}>
                ¿Cuántos depósitos tenés hoy?{' '}
                <span className="text-red-600">*</span>
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
                className={`${inputClass} max-w-[12rem]`}
              />
              <p className={fieldHintClass}>
                Podés ingresar 0 si aún no tenés depósitos.
              </p>
            </div>

            <div>
              <label
                htmlFor="waitlist-logistics-pain"
                className={fieldLabelClass}
              >
                ¿Cuáles son hoy tus mayores problemas en la logística en
                Argentina? (opcional)
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
                className={`${inputClass} min-h-[5.5rem] resize-y`}
              />
              <p className={fieldHintClass}>
                Hasta {WAITLIST_FIELD_MAX.logisticsPainPoints} caracteres.
              </p>
            </div>
          </section>

          <section className="mt-8 space-y-4 border-t border-gray-100 pt-8">
            {error ? (
              <p
                id="waitlist-form-alert"
                role="alert"
                className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-800"
              >
                {error}
              </p>
            ) : null}

            {doneMessage ? (
              <p
                role="status"
                className="rounded-lg border border-green-200 bg-green-50 px-3 py-2.5 text-sm text-green-900"
              >
                {doneMessage}
              </p>
            ) : null}

            {!canSubmit && !loading && !doneMessage ? (
              <p className="text-sm leading-relaxed text-gray-500">
                {getApiBaseUrl()
                  ? 'Completá las preguntas obligatorias, el rango de remitos por día, la cantidad de depósitos, nombre, empresa y tu correo para enviar.'
                  : 'Falta configurar NEXT_PUBLIC_API_URL: el envío no está disponible hasta que esté la URL de la API.'}
              </p>
            ) : null}

            <label className={checkboxOptionClass}>
              <input
                type="checkbox"
                name="product_updates_opt_in"
                checked={productUpdatesOptIn}
                onChange={(e) => setProductUpdatesOptIn(e.target.checked)}
                className="mt-1 h-4 w-4 shrink-0 rounded border-gray-300 text-blue-600 focus:ring-blue-500/30"
              />
              <span className="min-w-0">
                <span className={`block ${questionLabelClass}`}>
                  Quiero recibir novedades del producto y acceso anticipado.
                </span>
                <span className={`block ${sectionDescClass}`}>
                  Sin spam; podés darte de baja cuando quieras.
                </span>
              </span>
            </label>

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
              className="inline-flex w-full items-center justify-center rounded-lg bg-blue-600 px-4 py-3.5 text-base font-semibold text-white shadow-sm transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? 'Enviando…' : 'Unirme a la lista'}
            </button>

            <p className="text-center text-xs leading-relaxed text-gray-500 sm:text-sm">
              Al enviar, aceptás nuestra{' '}
              <Link
                href="/privacidad"
                className="font-medium text-blue-600 underline-offset-2 hover:underline"
              >
                Política de privacidad
              </Link>{' '}
              y el tratamiento de tus datos para gestionar tu solicitud en la
              lista de espera.
            </p>
          </section>
        </div>
      </div>
    </form>
  );
}
