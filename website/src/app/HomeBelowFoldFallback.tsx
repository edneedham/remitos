/** Lightweight placeholders while GSAP sections stream in (home page). */
export default function HomeBelowFoldFallback() {
  return (
    <div
      className="border-b border-gray-200 bg-white py-16 px-4 sm:px-6 lg:px-8"
      aria-hidden
    >
      <div className="mx-auto max-w-6xl animate-pulse space-y-8">
        <div className="h-9 max-w-sm rounded-lg bg-gray-200" />
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="h-72 rounded-2xl bg-gray-100" />
          <div className="h-72 rounded-2xl bg-gray-100" />
        </div>
      </div>
    </div>
  );
}
