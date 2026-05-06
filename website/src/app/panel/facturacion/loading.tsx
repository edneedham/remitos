import { PanelBillingSkeleton } from '../components/PanelSkeletons';

export default function FacturacionLoading() {
  return (
    <>
      <span className="sr-only">Cargando facturación…</span>
      <PanelBillingSkeleton />
    </>
  );
}
