# Beta Final - v0.1.15

**Estado:** Completado y etiquetado como `v0.1.15-offline-beta`

Esta versión representa el estado final de la aplicación offline. La rama `main` permanecerá en modo offline-only, mientras que el desarrollo de integración con backend continúa en la rama `backend-integration`.

## Características Completadas

### 1) Estabilidad e integridad de datos
- ✅ Centralización de claves de campos OCR y estados como constantes
- ✅ Pantalla de detalle/edición de ingresos con imagen escaneada
- ✅ Flujo de repartos con estados por línea (entregado/devuelto) y cierre de lista
- ✅ Historial de auditoría con timestamps

### 2) Performance
- ✅ Índices de base de datos para búsquedas rápidas
- ✅ Paginación en historiales
- ✅ Debouncing en filtros de búsqueda
- ✅ Escalado de imágenes al guardar y al cargar
- ✅ Caché de imágenes decodificadas

### 3) Mejoras de UI
- ✅ Vista previa post-captura con rotación/recorte
- ✅ Búsqueda con filtros por estado y fechas
- ✅ Indicador de uso de almacenamiento

## Tareas Pendientes (Backend Integration)

Estas tareas se desarrollarán en la rama `backend-integration`:

### Export y respaldo
- [ ] Exportar historiales (CSV/PDF) con hoja de compartir
- [ ] Configuración de retención de imágenes y limpieza opcional

### Mejoras de flujo de escaneo
- [ ] Modo batch: encolar capturas, revisar después
- [ ] "Guardar y escanear siguiente" y entrada manual sin OCR

### Visibilidad
- [ ] Filtros con selector de fecha y chips de filtro activos
- [ ] Badges de estado y contadores en tarjetas de historial

### Integración con Backend
- [ ] Cliente HTTP para API backend
- [ ] OCR híbrido: backend + fallback local
- [ ] Sincronización de auditoría
- [ ] Subida de imágenes para análisis mejorado

## Sistema de Feature Flags

Se ha implementado `FeatureFlags` para controlar gradualmente la integración:

```kotlin
// En main (offline-only)
FeatureFlags.configureOfflineMode()

// En backend-integration
FeatureFlags.configureBackendMode("https://api.example.com")
```

Esto permite que la rama `main` siga funcionando 100% offline mientras se desarrolla la integración.

## Próximos Pasos

1. **Rama `main`:** Solo correcciones de errores críticos, manteniendo modo offline
2. **Rama `backend-integration`:** Desarrollo activo de integración con backend
3. **Tag `v0.1.15-offline-beta`:** Punto de referencia estable para la versión offline
