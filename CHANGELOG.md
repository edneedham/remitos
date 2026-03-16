# Changelog

## [0.2.0-alpha03] - 2026-03-12

### Android App Refactoring & Improvements

This release focuses on comprehensively improving the Android application's architecture, build system, and code quality.

- **Architecture & Separation of Concerns**: 
  - Split the monolithic `RemitosRepository` God object into domain-specific repositories (`InboundRepository`, `OutboundRepository`, `AuditRepository`) using a Facade pattern. This safely decouples logic while maintaining backward compatibility.
  - Hoisted complex draft state and form mutations out of the `OutboundListScreen` composable into the `OutboundViewModel`, strictly enforcing a Unidirectional Data Flow pattern.
  - Refactored `InboundViewModel` to use an MVI-style `StateFlow` coupled with a unified `UiState` data class. This eliminates multiple disconnected Compose `mutableStateOf` properties in the ViewModel layer.
  
- **Build System & Gradle**: 
  - Migrated Room database compilation from KAPT to KSP (Kotlin Symbol Processing), resulting in significantly faster build times.
  - Updated Google ML Kit dependencies (`text-recognition` and `barcode-scanning`) to resolve 16 KB memory page alignment warnings, protecting the app from fatal crashes on upcoming Android 15+ devices.
  - Enabled Room schema exports and versioned the database schema to ensure safe automated migration testing.
  
- **Critical Bugs & API Level Violations**: 
  - Replaced manual UI image processing with **Coil** for efficient memory management, disk caching, and optimal downsampling.
  - Fixed a fatal `NoSuchMethodError` crash occurring on Android 8.0/8.1 devices by removing API 28+ `ImageDecoder` usages in favor of a backward-compatible `BitmapFactory` fallback utility.
  
- **Code Smells & UI Practices**: 
  - Extracted all hardcoded Spanish strings from major Compose UI screens (`DashboardScreen`, `InboundDetailScreen`, `InboundScanScreen`, `OutboundListScreen`) into `res/values/strings.xml` to enhance maintainability.
  - Replaced boxed primitive UI states (e.g., `mutableStateOf(0L)`) with unboxed equivalents (`mutableLongStateOf()`) across the app to reduce JVM boxing/unboxing overhead during recompositions.
  - Improved exception handling in `InboundViewModel` to catch specific exceptions (like `IOException`) while explicitly re-throwing `CancellationException` to avoid silently breaking Coroutine contexts.
