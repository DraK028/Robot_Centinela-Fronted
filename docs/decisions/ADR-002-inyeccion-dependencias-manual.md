# ADR-002: Inyección de dependencias manual

- **Estado:** aceptada
- **Fecha:** 2026-10-15

## Contexto

La documentación no exige un framework de DI y pide no generar nada que no esté explícito
(`Initial_promt.md`). El expediente (§14.1) solo pide una capa `composition` que enlace las
implementaciones.

## Decisión

`AppContainer` (`app/.../shell/AppContainer.kt`) construye a mano los adaptadores y los casos de uso.
`CentinelaApplication` lo crea de forma diferida.

## Consecuencias

- No hay dependencias adicionales ni generación de código.
- Toda la composición es visible en un solo archivo.
- Si la UI crece y la composición se complica, se puede adoptar un framework de DI sin cambiar
  `:core`.
