# Remitos Monorepo

Aplicación para el manejo de remitos y repartos: **app Android** (escaneo OCR y operación en depósito), **API Go** (sync, autenticación, facturación y AFIP/ARCA opcional), y **sitio web** Next.js (marketing, registro, panel de cuenta y facturación).

## Estructura del monorepo

```
remitos/
├── android/          # App Android (Kotlin + Jetpack Compose)
├── backend/          # API REST (Go), migraciones SQL, jobs
│   ├── db/migrations/
│   ├── internal/
│   ├── main.go
│   └── docker-compose.yml   # solo Postgres local (ver abajo)
├── website/          # Sitio Next.js (App Router)
├── scripts/
└── README.md
```

## Android

App para escaneo de remitos con OCR, notas de ingreso, listas de reparto y seguimiento de entregas; funciona **offline-first** con sync opcional al backend.

### Estado actual (referencia)

**Versión beta offline (0.1.15)** — operaciones locales completas; sync condicionado por flags.

**Tag de referencia:** `v0.1.15-offline-beta`

### Características principales

- **Ingresos:** OCR (ML Kit + OpenCV), códigos GS1, exportación CSV, edición y bultos.
- **Repartos:** listas, asignación, checklist y cierre.
- **Auditoría:** historial de cambios y ediciones.

### Tecnologías

Jetpack Compose, Room, ML Kit, MVVM.

### Build

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Backend (API Go)

Servicio HTTP con **PostgreSQL**, **JWT** para sesiones web/móvil, sync multi-tenant, **Mercado Pago** para cobros y suscripciones, y —cuando está configurado— integración directa **AFIP/ARCA** (padrón de CUIT, comprobantes con CAE). Las migraciones se aplican al **arranque** del proceso API (`runMigrations` en `main.go`).

### Versión de Go

El backend **requiere Go 1.25.x** (`go 1.25.0` en `backend/go.mod`; las dependencias GCP también piden ≥ 1.25). **Instalalo antes de `go run`**: paquete oficial para macOS/Linux/Windows en [go.dev/dl](https://go.dev/dl) (evita depender del auto-download del comando `go`, que puede dar timeout feo en redes lentas o bloqueadas).

Si ya tenés una versión vieja en el PATH, verificá con `go version` que sea **1.25**. Si el download automático de toolchain falla igual, revisá **proxy/firewall** y `GOPROXY`; **`GOTOOLCHAIN=local` con Go 1.24 no sirve** porque el código no compila sin 1.25.

### Configuración local

Copiá variables desde `backend/.env.example`. Para Postgres solo:

```bash
cd backend
docker compose up -d   # levanta Postgres según docker-compose.yml
```

Levantá la API con tu `.env` apuntando a ese Postgres (no commitear secretos).

### Tests

```bash
cd backend
go test ./...
```

## Sitio web (`website/`)

Panel y flujos públicos en **Next.js**; usa `NEXT_PUBLIC_API_URL` para hablar con la API. Ver `website/.env.example`.

```bash
cd website
pnpm install
pnpm dev
```

La rama principal de trabajo es **`main`**.

## Documentación operativa

| Documento | Contenido |
|-----------|-----------|
| **`To-Prod.md`** | Despliegue (Neon, Vercel, Cloud Run), env y smoke checks. |
| **`OPERATIONS.md`** | Migraciones, `/health` vs `/health/ready`, webhooks, AFIP. |
| **`billing-doc.md`** | Modelo de facturación, MEP, renovaciones. |
| **`FEATURE_FLAGS_MATRIX.md`** | Alineación Android / API / web antes de releases. |

## Feature flags (Android)

Control de modo offline vs backend; ver `FEATURE_FLAGS_MATRIX.md`.

```kotlin
FeatureFlags.configureOfflineMode()
FeatureFlags.configureBackendMode("https://tu-api.example.com")
```

Flags típicos: `enableBackendOcr`, `enableImageUpload`, `enableCloudSync`.

## Licencia

Proyecto privado — todos los derechos reservados.
