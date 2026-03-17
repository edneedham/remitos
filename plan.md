# Remitos: Role Separation & Admin Features Plan

## Phase 1: Dashboard & Navigation Restrictions
- **Description:** Implement Role-Based Access Control (RBAC) on the main dashboard and establish routing for new Admin screens.
- **Tasks:**
  - [x] Retrieve the current user's role via `AuthManager` within `DashboardScreen`.
  - [x] Hide the `Actividad` (Metrics/Reports) tile for users with the `operator` role.
  - [x] Create a new `Administración` section in `DashboardScreen`, visible only to `admin` users.
  - [x] Add two action tiles to this new section: `Gestión de Usuarios` and `Plantillas de Documentos`.
  - [x] Update `RemitosApp.kt` navigation to support the two new routes (initially pointing to empty placeholder screens).

## Phase 2: Restrict Operator Permissions
- **Description:** Enforce read-only or minor-edit limits for operators when viewing historical records.
- **Tasks:**
  - [x] Update `InboundDetailScreen` / `InboundDetailViewModel`: Hide or disable the "Anular ingreso" (Void) button if the current user is an `operator`. Ensure `admin`s retain this ability.
  - [x] Review `OutboundHistoryScreen` / `InboundHistoryScreen` to ensure destructive actions are restricted to `admin`.
  - [x] Verify that operators can still perform their required tasks (e.g., status updates) without being blocked.

## Phase 3: Implement User Management (Backend + Android)
- **Description:** Build full-stack functionality for an Admin to view and manage operator accounts.
- **Backend Tasks:**
  - [x] Update `admin.go` to include endpoints for fetching operators (`GET /admin/operadores`), deactivating them (`PUT /admin/operadores/{id}/status`), and resetting passwords (`PUT /admin/operadores/{id}/password`).
  - [x] Add repository methods to support these queries and updates.
- **Android Tasks:**
  - [x] Add API definitions to `RemitosApiService.kt` to interact with the new backend endpoints.
  - [x] Update `SyncManager` to sync the full list of operators locally into `LocalUserDao` for offline validation.
  - [x] Create `UserManagementScreen.kt` and its ViewModel.
  - [x] Implement a list view of all users queried from `LocalUserDao`.
  - [x] Implement a form/dialog to create new operators (calling the backend `POST /admin/operadores`).
  - [x] Implement actions for Admins to deactivate accounts or reset operator passwords (calling backend endpoints and updating local db).

## Phase 4: Template Configuration & PDF Generation (Admin Only)
- **Description:** Allow Admins to customize the printed Driver Checklist (Outbound List).
- **Tasks:**
  - [x] Create `TemplateConfigScreen.kt` and its ViewModel.
  - [x] Build UI for customizing templates:
    - [x] **Logo Selection:** Use `ActivityResultContracts.GetContent` to allow choosing an image from the device, save the URI locally.
    - [x] **Column Toggles:** Checkboxes to hide/show specific columns on the printed grid (e.g., "Peso", "Volumen", "Observaciones").
    - [x] **Legal Text:** A text field for custom footer text.
  - [x] Save preferences locally using `SharedPreferences` or `DataStore`.
  - [x] Update `OutboundListPrinter.kt`:
    - [x] Read saved preferences before drawing the Canvas.
    - [x] Render the logo image if a valid URI is saved.
    - [x] Dynamically skip and re-scale columns based on toggles.
    - [x] Draw the custom legal text at the bottom of the page.

## Phase 5: Data Export to CSV (Admin Only)
- **Description:** Provide an export mechanism for reporting.
- **Tasks:**
  - [x] In `ActivityScreen` (visible only to `admin`), add an "Exportar a CSV" button.
  - [x] Create a utility function to format the queried records (`InboundNoteEntity` or `OutboundListEntity` with lines) into a valid CSV string.
  - [x] Write the CSV string to a temporary file in the app's cache directory.
  - [x] Use `Intent.ACTION_SEND` to trigger the Android Share Sheet, allowing the Admin to save the CSV or share it.
