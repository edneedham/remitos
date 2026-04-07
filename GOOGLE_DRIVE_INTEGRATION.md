# Google Drive CSV Export Integration

## Overview
This implementation adds Google Drive upload functionality to the CSV export feature in the Remitos app. Users can now choose to export CSV files locally (Downloads) and optionally upload them to Google Drive.

## Features

### 1. Export Options
- **Save locally only**: Export CSV to device Downloads folder (existing behavior)
- **Save and upload**: Export locally AND upload to Google Drive

### 2. Google Drive Integration
- **Sign-in**: Users sign in with their Google account via Google Sign-In
- **Default folder**: Files uploaded to "Remitos Exports" folder (auto-created if missing)
- **Permissions**: Uses `DRIVE_FILE` scope for file access only (no full Drive access)

### 3. Offline Handling (Simple Approach)
- Always saves locally first
- If offline when trying to upload: Shows error but keeps local file
- User can retry export later when online

## UI Flow

1. User taps Export button (floating action button)
2. Export dialog appears with:
   - Filename input field (auto-populated)
   - If not signed in: "Iniciar sesión con Google" button
   - If signed in: Checkbox "Subir CSV a Google Drive"
3. User chooses export option and taps button
4. Show success/error states with option to view in Drive

## Files Modified/Created

### New Files
- `/android/app/src/main/java/com/remitos/app/drive/GoogleDriveManager.kt`
  - Handles Google Sign-In and Drive API operations
  - Uploads CSV files to Drive
  - Creates "Remitos Exports" folder if needed

### Modified Files
1. **build.gradle.kts**
   - Added Google Sign-In and Drive API dependencies

2. **strings.xml**
   - Added 13 new strings in Argentine Spanish for Drive UI

3. **InboundDetailScreen.kt**
   - Updated Export dialog with Drive options
   - Added Google Sign-In launcher
   - Shows upload progress and success/error states

4. **InboundDetailViewModel.kt**
   - Added Google Drive state management
   - Updated `exportToCsv()` to support Drive upload
   - Added `handleGoogleSignInResult()` and `checkGoogleSignInStatus()`

## Dependencies Added
```kotlin
implementation("com.google.android.gms:play-services-auth:21.1.0")
implementation("com.google.http-client:google-http-client-gson:1.44.1")
implementation("com.google.api-client:google-api-client-android:2.4.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20240809-2.0.0")
```

## String Resources (Argentine Spanish)
- `exportar_a_google_drive`
- `subir_csv_a_google_drive`
- `carpeta_por_defecto` (Default: Remitos Exports)
- `exportar` / `exportar_y_subir` / `solo_guardar_locamente`
- `iniciar_sesion_con_google`
- `debes_iniciar_sesion_para_subir_a_drive`
- `exportado_a_descargas`
- `exportado_y_subido_a_drive`
- `error_al_subir_a_drive`
- `reintentar`
- `ver_en_drive`
- `guardado_localmente_sin_conexion`
- `error_de_conexion_no_se_pudo_subir_a_drive`

## Notes
- Google Sign-In API is deprecated but still functional
- For production, consider migrating to newer Credential Manager API
- Folder permissions are managed by business admins via Google Drive web UI
- Works with both individual Google accounts and Google Workspace accounts

## Testing Checklist
- [ ] Export CSV without Drive upload (local only)
- [ ] Export CSV with Drive upload (signed in)
- [ ] Sign in to Google from export dialog
- [ ] Handle offline scenario (shows error but saves locally)
- [ ] View uploaded file in Drive (opens browser)
- [ ] Verify "Remitos Exports" folder is created
