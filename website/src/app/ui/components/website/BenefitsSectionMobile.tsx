'use client';

import Image from 'next/image';
import { useId, useState } from 'react';
import {
  FIRST_BENEFIT_LINE,
  PANEL_CAPTION_LINE,
  SECOND_BENEFIT_LINE,
  THIRD_BENEFIT_BEFORE,
  THIRD_BENEFIT_PRODUCT,
} from './benefitsContent';

type PreviewTab = 'app' | 'panel';

const benefitCardClass =
  'border-l-[3px] border-blue-600 pl-4 text-left text-lg font-semibold leading-snug tracking-tight text-gray-900';

export default function BenefitsSectionMobile() {
  const [tab, setTab] = useState<PreviewTab>('app');
  const appTabId = useId();
  const panelTabId = useId();
  const appPanelId = useId();
  const panelPanelId = useId();

  return (
    <div className="mx-auto w-full max-w-[68.8rem] space-y-5">
      <ul className="space-y-3" role="list">
        <li>
          <p className={benefitCardClass}>{FIRST_BENEFIT_LINE}</p>
        </li>
        <li>
          <p className={benefitCardClass}>{SECOND_BENEFIT_LINE}</p>
        </li>
        <li>
          <p
            className={`${benefitCardClass} flex flex-wrap items-baseline gap-x-2 gap-y-1`}
          >
            <span>{THIRD_BENEFIT_BEFORE}</span>
            <span className="inline-flex items-center gap-1.5">
              <Image
                src="/brands/google-drive-logo.png"
                alt=""
                width={24}
                height={24}
                className="size-6 shrink-0 translate-y-0.5"
                aria-hidden
              />
              <span className="whitespace-nowrap">{THIRD_BENEFIT_PRODUCT}</span>
            </span>
            <span aria-hidden>.</span>
          </p>
        </li>
      </ul>

      <div className="space-y-3">
        <div
          role="tablist"
          aria-label="Vista previa del producto"
          className="inline-flex w-full rounded-lg border border-gray-200 bg-gray-50 p-1"
        >
          <button
            type="button"
            role="tab"
            id={appTabId}
            aria-selected={tab === 'app'}
            aria-controls={appPanelId}
            onClick={() => setTab('app')}
            className={`flex-1 rounded-md px-3 py-2 text-sm font-semibold transition-colors ${
              tab === 'app'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            App
          </button>
          <button
            type="button"
            role="tab"
            id={panelTabId}
            aria-selected={tab === 'panel'}
            aria-controls={panelPanelId}
            onClick={() => setTab('panel')}
            className={`flex-1 rounded-md px-3 py-2 text-sm font-semibold transition-colors ${
              tab === 'panel'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Panel web
          </button>
        </div>

        <div
          role="tabpanel"
          id={appPanelId}
          aria-labelledby={appTabId}
          hidden={tab !== 'app'}
          className="flex justify-center"
        >
          <div className="overflow-hidden rounded-2xl p-3">
            <Image
              src="/screenshots/dashboard.png"
              alt="Vista del dashboard de la aplicación En Punto"
              width={286}
              height={611}
              className="h-auto w-[200px] rounded-xl"
              sizes="200px"
            />
          </div>
        </div>

        <div
          role="tabpanel"
          id={panelPanelId}
          aria-labelledby={panelTabId}
          hidden={tab !== 'panel'}
          className="space-y-5 px-3 pt-2"
        >
          <div className="relative overflow-hidden rounded-md bg-white p-4 shadow-[0_6px_16px_rgba(15,23,42,0.12)]">
            <div className="pointer-events-none absolute inset-0 -z-10 rounded-md bg-gradient-to-br from-blue-200/30 via-transparent to-indigo-300/20 blur-xl" />
            <Image
              src="/screenshots/web-panel-screenshot.png"
              alt="Panel web de administración En Punto"
              width={3002}
              height={1651}
              className="h-auto w-full rounded-md object-contain"
              sizes="100vw"
            />
          </div>
          <p className="px-2 text-center text-lg font-semibold leading-snug tracking-tight text-gray-900">
            {PANEL_CAPTION_LINE}
          </p>
        </div>
      </div>
    </div>
  );
}
