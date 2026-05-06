import { PanelDashboardSkeleton } from './components/PanelSkeletons';

/** Default while the main dashboard segment resolves. */
export default function PanelLoading() {
  return (
    <>
      <span className="sr-only">Cargando panel…</span>
      <PanelDashboardSkeleton />
    </>
  );
}
