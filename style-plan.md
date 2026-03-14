# Styling Unification Plan

This plan outlines the steps required to unify the visual styling across the Remitos Android application. The goal is to enforce a consistent look and feel using the existing Material 3 design system, eliminate hardcoded values, and maximize the reuse of shared components.

## Phase 1: Color & Theme Token Enforcement

**Goal:** Ensure all screens respect the centralized Material Theme and semantic color palette defined in `ui/theme/Color.kt`, rather than defining their own local colors.

*   [ ] **Purge Hardcoded Hex Codes**: 
    *   Target: `BarcodeScanningScreen.kt`, `InboundDetailScreen.kt`.
    *   Action: Replace all instances of hardcoded success/warning colors (e.g., `Color(0xFFE8F5E9)`, `Color(0xFF2E7D32)`, `Color(0xFFE65100)`) with the semantic colors already provided in `Color.kt` (e.g., `Success100`, `Success500`, `Warning100`).
*   [ ] **Replace Explicit Colors (`Color.White`, `BrandBlue`)**:
    *   Target: `OutboundListScreen.kt`, `DashboardScreen.kt`, `LoginScreen.kt`.
    *   Action: Replace raw `Color.White` usages with `MaterialTheme.colorScheme.surface` or `background` as appropriate. Replace manual `BrandBlue` usages with `MaterialTheme.colorScheme.primary`.
    *   Action: Remove the dummy white-on-white vertical gradient from `LoginScreen.kt`.

## Phase 2: Standardizing Layout Dimensions

**Goal:** Provide a consistent rhythm and rhythm to the UI so screens don't feel different when navigating between them.

*   [ ] **Create `Spacing.kt` File**:
    *   Target: `ui/theme/Spacing.kt`
    *   Action: Introduce semantic spacing tokens based on standard increments (e.g., `Spacing.ScreenPadding = 20.dp`, `Spacing.SectionSpacing = 16.dp`, `Spacing.ItemSpacing = 8.dp`). Add it to the CompositionLocal wrapper for theming if desired, or simply expose it as a standard object.
*   [ ] **Apply Uniform Structural Padding**:
    *   Target: All major screens (`LoginScreen`, `DashboardScreen`, `InboundDetailScreen`, `OutboundListScreen`, `BarcodeScanningScreen`).
    *   Action: Refactor the top-level `Modifier.padding(...)` applied within `Scaffold` blocks to consistently use the new `Spacing.ScreenPadding`.
*   [ ] **Standardize Vertical & Horizontal Alignments**:
    *   Target: Column and Row `Arrangement.spacedBy(...)` usages across the app.
    *   Action: Standardize the disparate spacing values (`8.dp`, `10.dp`, `12.dp`, `16.dp`) into predictable standard dimensions defined in `Spacing.kt`.

## Phase 3: Component Consolidation & Shape Enforcement

**Goal:** Force the UI to rely on flexible, shared internal components rather than declaring one-off "raw" Compose elements that drift out of sync.

*   [ ] **Enforce `RemitosTextField`**:
    *   Target: `LoginScreen.kt`, `OutboundListScreen.kt` (Dropdown implementations).
    *   Action: Replace massive, manually styled `OutlinedTextField` implementations with the shared `RemitosTextField`. Ensure `RemitosTextField` is flexible enough to handle these use cases (e.g., passing trailing icons and supporting read-only dropdown states).
*   [ ] **Consolidate and Deprecate Local Cards**:
    *   Target: `ui/components/SectionCard.kt`, `ui/components/RemitosCard.kt`, `DashboardScreen.kt`, `OutboundListScreen.kt`.
    *   Action: Update `SectionCard` and `RemitosCard` to accept customizable parameters for elevation and background color (defaulting to Material Theme surface colors instead of hardcoded white).
    *   Action: Delete the one-off `RepartoCard` defined locally in `OutboundListScreen` and the `ActionTile`/`PrimaryActionCard` defined in `DashboardScreen`, swapping them out for the unified shared components.
*   [ ] **Enforce Material Shapes**:
    *   Target: All screens.
    *   Action: Search for explicit `RoundedCornerShape(...)` declarations (e.g., in `InboundDetailScreen.kt` for clipping images) and replace them with `MaterialTheme.shapes.medium` (or small/large respectively) to guarantee border radius consistency across the OS and app.
