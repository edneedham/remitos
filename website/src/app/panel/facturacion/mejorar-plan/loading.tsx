import { PanelFormSkeleton } from '../../components/PanelSkeletons';

export default function MejorarPlanLoading() {
  return (
    <>
      <span className="sr-only">Cargando mejorar plan…</span>
      <PanelFormSkeleton />
    </>
  );
}
