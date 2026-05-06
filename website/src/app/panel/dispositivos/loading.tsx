import { PanelDevicesRouteSkeleton } from '../components/PanelSkeletons';

export default function DispositivosLoading() {
  return (
    <>
      <span className="sr-only">Cargando dispositivos…</span>
      <PanelDevicesRouteSkeleton />
    </>
  );
}
