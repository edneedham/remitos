# Remitos Monorepo

Aplicación completa para el manejo de remitos y repartos. Incluye aplicación Android para escaneo OCR y backend Go para sincronización y API.

## Estructura del Monorepo

```
remitos/
├── android/          # Aplicación Android (Kotlin + Jetpack Compose)
│   ├── app/          # Código fuente de la app
│   ├── build.gradle.kts
│   └── gradlew
├── backend/          # Servidor backend (Go)
│   ├── internal/     # Paquetes internos
│   ├── main.go       # Punto de entrada
│   ├── Dockerfile
│   └── docker-compose.yml
├── scripts/          # Scripts compartidos
└── README.md
```

## Android App

Aplicación Android para el manejo de remitos y repartos. Escanea remitos usando OCR, gestiona listas de reparto y realiza seguimiento de entregas.

### Estado Actual

**Versión Beta Offline (0.1.15)**

La aplicación funciona completamente offline con todas las operaciones locales:
- Escaneo de remitos con OCR en el dispositivo (ML Kit + OpenCV)
- Gestión de notas de ingreso y bultos
- Creación y cierre de listas de reparto
- Seguimiento de estados de entrega (en depósito, en tránsito, entregado)
- Escaneo de códigos de barras GS1
- Auditoría de cambios con historial completo

**Tag de la versión beta:** `v0.1.15-offline-beta`

### Características Principales

#### Ingresos
- Escaneo de remitos con captura de imagen
- OCR automático de campos (CUIT, nombre, dirección, etc.)
- Escaneo de códigos de barras GS1 para cada bulto
- Exportación a CSV
- Edición de campos capturados
- Seguimiento de bultos disponibles

#### Repartos
- Creación de listas de reparto
- Asignación de remitos a listas
- Firma de checklist
- Cierre de lista cuando todos los items están entregados
- Historial de estados por item

#### Auditoría
- Historial de cambios de estado
- Historial de ediciones de campos
- Fechas y razones de modificaciones

### Tecnologías

- **UI:** Jetpack Compose
- **Base de datos:** Room (SQLite)
- **OCR:** ML Kit Text Recognition + OpenCV (preprocesamiento)
- **Escaneo de códigos:** ML Kit Barcode Scanning
- **Arquitectura:** MVVM con ViewModels
- **Inyección de dependencias:** Manual (factory pattern)

### Construcción

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```

## Backend

Servidor Go para API REST y sincronización de datos con el backend.

### Características

- API REST para sincronización de remitos
- Autenticación JWT
- WebSocket para tiempo real
- Docker y docker-compose para deployment

### Construcción

```bash
cd backend
docker-compose up -d
```

## Arquitectura de Ramas

- **`main`** - Versión offline estable. Solo correcciones de errores críticos.
- **`backend-integration`** - Desarrollo activo para integración con backend.

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

## Licencia

Proyecto privado - Todos los derechos reservados
