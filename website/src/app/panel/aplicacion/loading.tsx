import { PanelListSkeleton } from '../components/PanelSkeletons';

export default function AplicacionLoading() {
  return (
    <>
      <span className="sr-only">Cargando aplicación…</span>
      <PanelListSkeleton rows={5} />
    </>
  );
}
