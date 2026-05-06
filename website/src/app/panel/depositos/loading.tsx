import { PanelListSkeleton } from '../components/PanelSkeletons';

export default function DepositosLoading() {
  return (
    <>
      <span className="sr-only">Cargando depósitos…</span>
      <PanelListSkeleton />
    </>
  );
}
