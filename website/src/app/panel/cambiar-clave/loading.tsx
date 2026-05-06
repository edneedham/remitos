import { PanelFormSkeleton } from '../components/PanelSkeletons';

export default function CambiarClaveLoading() {
  return (
    <>
      <span className="sr-only">Cargando cambiar contraseña…</span>
      <PanelFormSkeleton />
    </>
  );
}
