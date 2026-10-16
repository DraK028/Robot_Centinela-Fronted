# ADR-005: Interfaz en Jetpack Compose, simulador de datos y contratos provisionales

- **Estado:** propuesta
- **Fecha:** 2026-10-15

## Contexto

Llegó la propuesta de diseño de interfaces (sección 12, *Lunar Base*) y se pidió montar el frontend
sobre el backend hexagonal. Varios contratos de datos siguen pendientes (mapas, misiones,
capacidades, agregados) y no hay un proyecto de Supabase con datos para probar.

## Decisión

1. **Compose + Material 3** en `:app` (`ui/`), con navegación por tipos (Navigation 2.10) y
   `ViewModel` por pantalla. La inyección sigue siendo manual (ADR-002): las pantallas reciben el
   `AppContainer` por un `CompositionLocal`.
2. **Simulador como adaptador** (`shell/demo/` y `<dominio>/adapters/demo/`): implementa todos los
   puertos con datos sintéticos deterministas. Se elige con `centinela.dataSource` en
   `local.properties` (vacío = Supabase si está configurado, si no, simulador). Toda pantalla rotula
   los datos como simulados (HU15 CDA 2, RB29).
3. **Contratos provisionales en `:core`** para plano (`FloorPlanPort`) y misiones
   (`MissionQueryPort`), marcados como PROVISIONALES. Con Supabase son `null` y la interfaz muestra
   "contrato pendiente".
4. **compileSdk 37** (targetSdk sigue en 36): Compose 1.12, Lifecycle 2.11 y Navigation 2.10 se
   publican compilados con API 37.
5. **Tokens derivados** para cumplir el contraste que exige la guía, y **Poppins con dígitos
   tabulares** generada para el proyecto (ver `docs/ui/propuesta-frontend.md`).

## Consecuencias

- El equipo puede revisar la interfaz completa sin backend, con los casos de prueba de §12.6.
- Cuando backend publique los contratos de mapas y misiones, se ajustan `FloorPlan` y
  `MissionSummary` y se agregan sus adaptadores de Supabase; la interfaz no cambia de estructura.
- `:core` sigue sin depender de Android ni de Compose.
