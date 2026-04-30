import type { Entitlement } from '../dashboard/lib/entitlementTypes';

/** Set when the user opens la página de descarga de la app (`/dashboard/app`). */
export const CHECKLIST_DOWNLOAD_PAGE_VISITED_KEY =
  'remitos_checklist_download_page_visited';

export type TrialChecklistStepId = 'install' | 'login' | 'scan';

export type TrialChecklistStep = {
  id: TrialChecklistStepId;
  title: string;
  description: string;
  done: boolean;
};

export type TrialChecklistModel = {
  steps: TrialChecklistStep[];
  completedCount: number;
  total: number;
};

/**
 * Returns null when the checklist should be hidden (first scan already synced).
 */
export function buildTrialOnboardingChecklist(
  entitlement: Entitlement | null,
  downloadPageVisited: boolean,
): TrialChecklistModel | null {
  if (!entitlement) return null;

  const scanDone = (entitlement.remitos_processed_last_30_days ?? 0) >= 1;
  if (scanDone) return null;

  const devices = entitlement.device_count ?? 0;
  const installDone = downloadPageVisited || devices >= 1;
  const loginDone = devices >= 1;

  const steps: TrialChecklistStep[] = [
    {
      id: 'install',
      title: 'Descargá e instalá la app (Android)',
      description:
        'Obtené el APK desde la página de aplicación. La app de depósito es solo Android.',
      done: installDone,
    },
    {
      id: 'login',
      title: 'Iniciá sesión en el teléfono',
      description:
        'Usá el mismo código de empresa, correo o usuario y contraseña que en la web.',
      done: loginDone,
    },
    {
      id: 'scan',
      title: 'Escaneá tu primer remito',
      description:
        'Procesá un remito en la app; el contador de documentos se actualiza en este panel.',
      done: false,
    },
  ];

  const completedCount = steps.filter((s) => s.done).length;

  return {
    steps,
    completedCount,
    total: steps.length,
  };
}
