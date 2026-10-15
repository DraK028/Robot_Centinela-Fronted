# ADR-001: Arquitectura hexagonal con módulos `:core` y `:app`

- **Estado:** aceptada
- **Fecha:** 2026-10-15

## Contexto

El proyecto pide una primera versión del backend de la app con arquitectura hexagonal
(`Initial_promt.md`). El expediente exige Screaming Architecture por dominio, con puertos y
adaptadores (RNF16), y que el acceso a Supabase, MQTT y notificaciones quede separado de los casos
de uso (RNF18). La UI llegará después, del equipo de Ingeniería de Software.

## Decisión

Dos módulos Gradle:

- **`:core`** (Kotlin/JVM puro): `domain`, `ports` y `application` de cada dominio (`telemetry`,
  `environmentalmonitoring`, `robothealth`, `homemapping`, `missions`, `incidents`, `identity`). Solo
  depende de `kotlinx-coroutines-core`.
- **`:app`** (Android application): adaptadores Supabase por dominio y la raíz de composición
  (`shell`).

## Consecuencias

- El compilador impide que el dominio importe Android o Supabase.
- Las reglas se prueban en JVM, rápido y sin emulador.
- Los adaptadores y la composición comparten módulo. Si más adelante crecen (por ejemplo, un
  adaptador MQTT de prueba o notificaciones push), se pueden separar en un módulo `:adapters` sin
  tocar `:core`.
