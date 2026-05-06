import { PanelCompactSkeleton } from '../components/PanelSkeletons';

export default function PagoExitosoLoading() {
  return (
    <>
      <span className="sr-only">Cargando confirmación de pago…</span>
      <PanelCompactSkeleton />
    </>
  );
}
