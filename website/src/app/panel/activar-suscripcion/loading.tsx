import { PanelFormSkeleton } from '../components/PanelSkeletons';

export default function ActivarSuscripcionLoading() {
  return (
    <>
      <span className="sr-only">Cargando activar suscripción…</span>
      <PanelFormSkeleton />
    </>
  );
}
