# Android Refactoring and Improvement Plan

This document outlines the recommended actions to improve the structural, architectural, and code quality aspects of the `android` directory in the Remitos project. These improvements are categorized by impact and importance.

## 1. Critical Bugs & API Level Violations

- [x] **Fix `ImageDecoder` API Requirement:** `InboundDetailScreen.kt` and `OcrProcessor.kt` use `ImageDecoder` methods that require API 28+, while the project's `minSdkVersion` is 26. This causes fatal `NoSuchMethodError` crashes on Android 8.0/8.1 devices.
  - *Action:* Migrate image loading and decoding to a robust library like **Coil** (`AsyncImage`), or fallback to `BitmapFactory` for devices below API 28.
- [x] **Remove Manual Image Processing in UI:** The `InboundDetailScreen.kt` has manual bitmap caching, IO operations, and scaling integrated directly into the Composable.
  - *Action:* Delegate all UI image loading to Coil to automatically handle memory/disk caching, lifecycle management, and optimal downsampling.

## 2. Build System & Gradle

- [x] **Migrate KAPT to KSP:** The project currently uses `kapt` for Room compilation (`androidx.room:room-compiler`).
  - *Action:* Migrate to **KSP** (Kotlin Symbol Processing). It provides significant performance improvements and drastically reduces build times.
- [x] **Address 16 KB Page Alignment (Future-proofing):** The ML Kit native libraries (`barcode-scanning` and `text-recognition`) are flagged in lint as not being 16 KB aligned, which can crash the app on newer Android 15+ devices enforcing this standard.
  - *Action:* Update ML Kit dependencies to their latest available versions where Google has patched this for bundled SDKs.
- [x] **Enable Room Schema Export:** `AppDatabase` generates a compiler warning because schema export is not configured.
  - *Action:* Enable `room.schemaLocation` in `build.gradle.kts` and commit the schemas to version control to enable and enforce safe automated migration testing.

## 3. Architecture & Separation of Concerns

- [x] **Break Down "God" Repository:** `RemitosRepository.kt` has become a God object, managing everything from inbound notes and outbound lists to debug logs and allocation logic.
  - *Action:* Break it down into domain-specific repositories (e.g., `InboundRepository`, `OutboundRepository`, `SyncRepository`).
  - *Action:* Abstract complex cross-domain logic (like creating outbounds with inventory allocations) into dedicated Use Cases / Interactors.
- [x] **Hoist Business Logic from Composables:** Screens like `OutboundListScreen.kt` directly manage complex form mutations (adding, removing, editing draft lines, and ID counters) within the UI.
  - *Action:* Hoist this complex state completely into the `OutboundViewModel`. The UI should only render the state and emit events/intents to the ViewModel.
- [x] **Refactor ViewModel State Management:** ViewModels (like `InboundViewModel.kt`) rely heavily on Compose's `var ... by mutableStateOf(...)` for individual properties.
  - *Action:* Transition to the recommended MVI-style `StateFlow` approach (`MutableStateFlow<UiState>`). This ensures a single source of truth, avoids partial state updates, and cleanly decouples the ViewModel from Jetpack Compose.

## 4. Code Smells & UI Practices

- [ ] **Extract Hardcoded Strings:** There are over 130 instances of hardcoded Spanish strings directly embedded in Composables (e.g., `Text("Anular ingreso")`), severely impacting maintainability.
  - *Action:* Extract all hardcoded UI strings to `res/values/strings.xml` and use `stringResource(R.string.key)`.
- [x] **Fix State Autoboxing in Compose:** Several screens use `mutableStateOf(0L)` or `mutableStateOf(0)`, which causes continuous boxing/unboxing overhead on the JVM during recompositions.
  - *Action:* Swap these for primitive state holders: `mutableLongStateOf` and `mutableIntStateOf`.
- [x] **Handle Exceptions Specifically:** `InboundViewModel.processImage` catches a generic `Exception`, which can swallow unexpected system crashes (like `OutOfMemoryError`).
  - *Action:* Catch specific exceptions where expected and integrate a crash reporting mechanism (like Crashlytics) for unexpected failures.
