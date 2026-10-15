# ADR-003: supabase-kt y DTO según `JSON_estructura.txt`

- **Estado:** aceptada, con recursos provisionales
- **Fecha:** 2026-10-15

## Contexto

- El proyecto usa la API de Supabase y la estructura JSON de `JSON_estructura.txt`
  (`Initial_promt.md`).
- El expediente fija Supabase como persistencia común: Auth para personas, RLS y Realtime (§7.1).
- El esquema definitivo de Supabase **no está publicado**. El modelo de §13 es una propuesta que ya
  cambió.

## Decisión

1. Usar [supabase-kt](https://github.com/supabase-community/supabase-kt) 3.8.0 (Auth, Postgrest y
   Realtime) con el motor Ktor OkHttp.
2. El DTO del frame replica **exactamente** `JSON_estructura.txt` (`schema_version` 1.1) más
   `received_at` y `stream_version`, que asigna el backend (§7.2).
3. La app lee un recurso provisional `telemetry_frames_v1` (vista, tabla o RPC) con esa forma, y
   escucha con Realtime las inserciones en `telemetry_frames` (§7.4).
4. Membresías, robots y alertas se leen con los nombres y campos de §13.2, marcados como
   provisionales.
5. Todos los nombres viven en `SupabaseResources.kt`. Cambiarlos no afecta a `:core`.
6. Un frame que no cumple el contrato se devuelve como `FrameRecord.Rejected`. El cursor avanza y el
   rechazo se cuenta, en lugar de detener la sincronización.

## Consecuencias

- Cuando backend publique su contrato, el cambio se limita a `SupabaseResources.kt` y a los DTO.
- La autenticación es por correo y contraseña porque RF01 pide "recuperar acceso". Esta decisión se
  debe confirmar (docs/pendientes.md).
- La app nunca recibe claves privilegiadas: `SupabaseConfig` las rechaza (§7.3, RNF08).
