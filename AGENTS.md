# AGENTS.md

This repository is a Kotlin Android app using Jetpack Compose, Room, and ML Kit.
Follow this guide when making changes.

## Branch Strategy

**Main branches:**
- `main` - Offline-only beta (stable, feature-complete for standalone use)
- `backend-integration` - Active development for backend-connected features

**Tags:**
- `v0.1.15-offline-beta` - Final offline-only release

**Important:** When working on the `main` branch, keep it offline-only. Backend features go on `backend-integration`.

## Build, Lint, Test

All commands assume repo root.

- Build debug APK: `./gradlew :app:assembleDebug`
- Build release APK: `./gradlew :app:assembleRelease`
- Install debug on device/emulator: `./gradlew :app:installDebug`
- Lint (Android Lint): `./gradlew :app:lint`
- Unit tests (all): `./gradlew :app:test`
- Unit tests (debug): `./gradlew :app:testDebugUnitTest`
- Instrumentation tests (all): `./gradlew :app:connectedAndroidTest`

Run a single unit test (Gradle test filtering):
- Class: `./gradlew :app:testDebugUnitTest --tests "com.remitos.app.SomeTest"`
- Method: `./gradlew :app:testDebugUnitTest --tests "com.remitos.app.SomeTest.someMethod"`

Run a single instrumentation test:
- Class: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.remitos.app.SomeInstrumentedTest`

Notes:
- There is no custom lint or formatter configured (no ktlint/detekt yet).
- If you add new test sources, keep them under `app/src/test` (unit) or `app/src/androidTest` (instrumentation).

## Workflow

- Use git for all changes; keep commits small and focused on one concern.
- Push each commit to the GitHub remote after it is created.
- Always run tests after every change and report results.
- Prefer TDD for new parsing or business logic: write a failing test first, then implement.
- Avoid committing build artifacts or IDE files; keep `.idea/` untracked.
- Do not commit `opencv-sdk/` (use `scripts/fetch-opencv.sh` to download the SDK).

## Commit & Pull Request Guidelines

- Create commits with `scripts/committer "<msg>" <file...>`; avoid manual `git add`/`git commit` so staging stays scoped.

## Feature Flags

The app uses `FeatureFlags` object to control offline vs backend modes:

```kotlin
// Offline-only mode (default on main branch)
FeatureFlags.configureOfflineMode()

// Backend integration mode
FeatureFlags.configureBackendMode("https://api.example.com")
```

Available flags:
- `enableBackendOcr` - Use backend OCR instead of on-device
- `enableImageUpload` - Upload images to backend
- `enableCloudSync` - Sync audit logs with backend
- `backendBaseUrl` - Backend API base URL

**When adding features:**
- On `main`: Ensure they work with `FeatureFlags.configureOfflineMode()`
- On `backend-integration`: Can use backend features when flags are enabled

## Repo Structure

- `app/src/main/java/com/remitos/app`
  - `ui/` Compose UI and navigation
  - `data/` repository and data access
  - `data/db/` Room database, entities, and DAOs
  - `ocr/` ML Kit OCR processing
  - `print/` printing/PDF adapters

## Code Style Guidelines

### Kotlin & General Formatting

- Use 4-space indentation.
- Keep line length reasonable (aim for <= 120 chars when possible).
- One top-level class/object per file unless it is a tiny helper.
- Use trailing commas in multiline parameter lists for better diffs.
- Use standard Kotlin formatting; no custom formatter is enforced.

### Imports

- Use explicit imports; avoid wildcard imports.
- Order imports: Android/Jetpack first, then third-party, then project packages.
- Remove unused imports.

### Naming Conventions

- Packages: lowercase, no underscores.
- Classes/Interfaces: `PascalCase`.
- Functions/vars: `camelCase`.
- Constants: `UPPER_SNAKE_CASE` (top-level `const val`).
- Room table/column names use `snake_case` as in current entities.
- UI labels and visible strings should be Spanish.

### Types & Nullability

- Use non-null types by default; use nullable only when needed.
- Prefer `val` over `var` unless mutation is required.
- For IDs, use `Long` (Room auto-generated IDs).

### Error Handling

- Do not throw on the main/UI thread.
- Prefer sealed classes or `Result` for recoverable failures.
- Surface user-facing errors in Spanish with clear context.
- For ML Kit, handle failures and show a retry path.

### Coroutines & Threading

- Use `suspend` functions for DB and OCR operations.
- Execute DB work in Room's `withTransaction` where needed.
- Do not block the UI thread; use `Dispatchers.IO` where appropriate.

### Room / Database

- Keep entities minimal; add derived fields in view models, not entities.
- Use `@Transaction` when writing multiple tables.
- When creating `InboundPackage` rows, ensure atomicity.
- Store enum-like fields as strings (current pattern).

### Jetpack Compose

- Keep composables small and focused.
- Hoist state where possible; pass callbacks for actions.
- Avoid side effects in composables; use `LaunchedEffect` when needed.
- Prefer `Material3` components; keep labels in Spanish.

### OCR & Parsing

- CUIT format: `NN-NNNNNNNN-N` (regex `\b\d{2}-\d{8}-\d{1}\b`).
- Use keyword anchors in Spanish (e.g., "Remitente", "Destinatario").
- Track confidence per field; prompt user for missing data.

### Printing

- Use Android Print Framework (`PrintDocumentAdapter`).
- Ensure dates are formatted `dd/MM/yyyy` with `es-AR` locale.
- Keep the PDF layout simple; avoid non-ASCII unless required.

## UI Text & Localization

- Spanish only; do not introduce English labels.
- Prefer `strings.xml` for new UI text once the UI stabilizes.

## Testing Guidance

- Unit tests: focus on OCR parsing and repository logic.
- Instrumentation tests: focus on DB and basic UI flows.
- Add tests as new logic is introduced (especially parsing rules).

## Cursor/Copilot Rules

- No `.cursor/rules`, `.cursorrules`, or `.github/copilot-instructions.md` found in this repo.
