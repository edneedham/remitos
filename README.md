# Remitos App

Aplicación Android para el manejo de remitos y repartos. Escanea remitos usando OCR, gestiona listas de reparto y realiza seguimiento de entregas.

## Estado Actual

**Versión Beta Offline (0.1.15)**

La aplicación funciona completamente offline con todas las operaciones locales:
- Escaneo de remitos con OCR en el dispositivo (ML Kit + OpenCV)
- Gestión de notas de ingreso y bultos
- Creación y cierre de listas de reparto
- Seguimiento de estados de entrega (en depósito, en tránsito, entregado)
- Auditoría de cambios con historial completo

**Tag de la versión beta:** `v0.1.15-offline-beta`

## Arquitectura de Ramas

- **`main`** - Versión offline estable. Solo correcciones de errores críticos.
- **`backend-integration`** - Desarrollo activo para integración con backend.

## Características Principales

### Ingresos
- Escaneo de remitos con captura de imagen
- OCR automático de campos (CUIT, nombre, dirección, etc.)
- Edición de campos capturados
- Seguimiento de bultos disponibles

### Repartos
- Creación de listas de reparto
- Asignación de remitos a listas
- Firma de checklist
- Cierre de lista cuando todos los items están entregados
- Historial de estados por item

### Auditoría
- Historial de cambios de estado
- Historial de ediciones de campos
- Fechas y razones de modificaciones

## Tecnologías

- **UI:** Jetpack Compose
- **Base de datos:** Room (SQLite)
- **OCR:** ML Kit Text Recognition + OpenCV (preprocesamiento)
- **Arquitectura:** MVVM con ViewModels
- **Inyección de dependencias:** Manual (factory pattern)

## Feature Flags

El sistema de flags permite controlar el modo offline vs backend:

```kotlin
// Modo offline (predeterminado en main)
FeatureFlags.configureOfflineMode()

// Modo con backend (backend-integration)
FeatureFlags.configureBackendMode("https://api.example.com")
```

Flags disponibles:
- `enableBackendOcr` - Usar OCR del backend
- `enableImageUpload` - Subir imágenes al backend
- `enableCloudSync` - Sincronizar auditoría con backend

## Construcción

```bash
# Debug APK
./gradlew :app:assembleDebug

# Tests
./gradlew :app:testDebugUnitTest

# Instalación en dispositivo
./gradlew :app:installDebug
```

## Estructura del Proyecto

```
app/src/main/java/com/remitos/app/
├── ui/           # Pantallas y componentes Compose
├── data/         # Repositorios y acceso a datos
├── data/db/      # Entidades y DAOs de Room
├── ocr/          # Procesamiento OCR
└── print/        # Impresión de listas
```

## Licencia

Proyecto privado - Todos los derechos reservados
