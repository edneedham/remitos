import LoadingSpinner from '../ui/components/shared/LoadingSpinner';

/** Shown during client navigations between panel routes (instant feedback). */
export default function PanelLoading() {
  return (
    <div
      className="flex min-h-[40vh] items-center justify-center py-16"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <LoadingSpinner />
    </div>
  );
}
