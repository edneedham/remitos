import { PanelUpgradePlanRouteSkeleton } from '../../components/PanelSkeletons';

export default function MejorarPlanLoading() {
  return (
    <>
      <span className="sr-only">Cargando cambiar de plan…</span>
      <PanelUpgradePlanRouteSkeleton />
    </>
  );
}
