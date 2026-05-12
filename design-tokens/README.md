# Design Tokens

Single source of truth for visual design tokens shared by the En Punto Next.js
website and the Android Compose app.

## Files

- `tokens.json` — Authoritative tokens. Edit only this file.
- `../website/scripts/gen-tokens.mjs` — Emits the `@theme {…}` block consumed by
  Tailwind in [`website/src/app/globals.css`](../website/src/app/globals.css).
- `../android/app/scripts/gen-tokens.kts` — Emits
  [`android/app/src/main/java/com/remitos/app/ui/theme/Tokens.kt`](../android/app/src/main/java/com/remitos/app/ui/theme/Tokens.kt).
  Wired into the `:app:preBuild` Gradle lifecycle via the `genTokens` task.

## Regenerating outputs

From the repo root:

```bash
# Both at once
pnpm --filter enpunto-website gen:tokens
./android/gradlew -p android :app:genTokens
```

CI verifies the generated files are in sync with `tokens.json` and fails the
build otherwise.

## Adding a new token

1. Add the value to `tokens.json`.
2. Run both generators above.
3. Reference the new token from:
   - Web: as a CSS variable inside `@theme {…}` (e.g. `--color-brand`)
     or via Tailwind utilities that consume it (`bg-(--color-brand)`).
   - App: from `com.remitos.app.ui.theme.Tokens` (e.g. `Tokens.Color.brandPrimary`).
