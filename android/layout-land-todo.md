# Landscape Layout Implementation Plan

## Overview
Implement landscape-specific layouts for all screens using Compose's `LocalConfiguration` to detect orientation and adapt layouts efficiently using horizontal space.

## Approach
- Use `LocalConfiguration` to detect orientation
- Create responsive layouts that utilize horizontal space in landscape
- Maintain consistency across all screens

## Screens Implementation

### 1. LoginScreen
- **Portrait**: Stacked fields (current)
- **Landscape**: Header left (smaller), form fields right side-by-side

### 2. UnlockScreen
- **Portrait**: Centered vertical (current)
- **Landscape**: Centered horizontal layout

### 3. DeviceSetupScreen
- **Portrait**: Stacked form (current)
- **Landscape**: 2-column form fields

### 4. DashboardScreen
- **Portrait**: Full header, stacked cards (current)
- **Landscape**: Compact header, 2-column grid cards

### 5. InboundScanScreen
- **Portrait**: Camera full height (current)
- **Landscape**: Camera left (60%), form right (40%)

### 6. InboundHistoryScreen
- **Portrait**: Single column list (current)
- **Landscape**: 2-column grid

### 7. OutboundHistoryScreen
- **Portrait**: Single column list (current)
- **Landscape**: 2-column grid

### 8. OutboundListScreen
- **Portrait**: Stacked form (current)
- **Landscape**: 2-column form

### 9. SettingsScreen
- **Portrait**: Stacked list (current)
- **Landscape**: 2-column grid

### 10. ActivityScreen
- **Portrait**: Stacked list (current)
- **Landscape**: 2-column grid

### 11. InboundDetailScreen
- **Portrait**: Stacked layout (current)
- **Landscape**: Side-by-side sections

### 12. OutboundPreviewScreen
- **Portrait**: Stacked layout (current)
- **Landscape**: Side-by-side sections

### 13. InboundCameraScreen
- **Portrait**: Full camera (current)
- **Landscape**: Camera with overlay controls

## Implementation Helper

Create utility composable for orientation detection:
```kotlin
@Composable
fun rememberIsLandscape(): Boolean {
    val config = LocalConfiguration.current
    return config.orientation == Configuration.ORIENTATION_LANDSCAPE
}
```

## Priority Order

1. LoginScreen
2. UnlockScreen
3. DashboardScreen
4. InboundScanScreen
5. InboundHistoryScreen
6. OutboundHistoryScreen
7. OutboundListScreen
8. SettingsScreen
9. ActivityScreen
10. InboundDetailScreen
11. OutboundPreviewScreen
12. InboundCameraScreen
