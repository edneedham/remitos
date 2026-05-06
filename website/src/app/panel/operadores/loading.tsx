import { PanelListSkeleton } from '../components/PanelSkeletons';

export default function OperadoresLoading() {
  return (
    <>
      <span className="sr-only">Cargando operadores…</span>
      <PanelListSkeleton rows={7} />
    </>
  );
}
