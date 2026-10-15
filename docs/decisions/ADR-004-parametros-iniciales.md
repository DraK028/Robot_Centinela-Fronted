# ADR-004: Parámetros iniciales configurables

- **Estado:** aceptada
- **Fecha:** 2026-10-15

## Contexto

El expediente da valores **iniciales** que se deben ajustar con pruebas reales. No son límites
garantizados (RNF04, §7.4). Los umbrales de alertas viven en la base de datos, no en la app.

## Decisión

Cada valor es un parámetro con default en el constructor de su política. Ninguno está fijo en la
lógica:

| Parámetro | Clase | Valor inicial | Origen |
|---|---|---|---|
| Periodo esperado de muestras | `FreshnessPolicy.defaultExpectedPeriod` | 2 s | §7 y §7.4: un frame cada 2 s |
| Periodos para "atrasado" | `FreshnessPolicy.stalePeriods` | 3 | RNF04 |
| Ingesta interrumpida | `IngestConnectivityPolicy.offlineAfter` | 10 s | RNF04 |
| Reconciliación periódica | `SyncConfig.reconciliationInterval` | 30 s | §7.4 |
| Ventana de deduplicación | `SyncConfig.dedupeWindow` | 2,700 frames | §7.5: 90 min a 2 s |
| Tamaño de página de deltas | `SyncConfig.deltaPageSize` | 100 | Provisional; no documentado |
| Zona horaria por defecto | `UtcInterval.DEFAULT_ZONE` | America/Mexico_City | RNF13 |

Si el catálogo de capacidades define el periodo de un canal (`channels.period_ms`), ese valor reemplaza
al periodo por defecto.

## Consecuencias

- Ajustar un valor no requiere cambiar la lógica, solo la composición (`AppContainer`).
- La app no contiene umbrales de gas ni de salud (RB08, RB23).
