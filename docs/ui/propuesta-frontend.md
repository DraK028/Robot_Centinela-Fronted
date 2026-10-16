# Propuesta de frontend Android (Jetpack Compose)

**Fuentes:** "Propuesta de diseño de interfaces" (sección 12, identidad *Lunar Base*) e
`Historias_de_Usuario_CDA_Corregido.xlsx`. **Backend:** el `:core` hexagonal de este repositorio.

La interfaz vive en `app/src/main/kotlin/com/robotcentinela/app/ui/` y solo habla con los casos de
uso de `:core` a través de `AppContainer`. No calcula alertas, no convierte ADC a ppm y no inventa
posiciones: muestra lo que el dominio afirma.

## Cómo probarla

1. Abre el proyecto en Android Studio (versión con AGP 9.4 y SDK de Android 37 instalado).
2. Sin `supabase.url` en `local.properties`, la app arranca con el **simulador** (rotulado "Datos
   simulados" en todas las pantallas, HU15). Para forzarlo: `centinela.dataSource=demo`.
3. Inicia sesión con cualquier correo y una contraseña de 6 caracteres o más.
4. En **Ajustes** (engrane de la cabecera) cambia el **escenario de simulación** para ver los tres
   casos de prueba que pide §12.6.

| Escenario | Qué se ve |
|---|---|
| Datos completos | Frames cada 2 s, MQ6/MQ4 en ADC "Sin calibración", CO "No instalado", batería solo con voltaje |
| Sensores ausentes o en fallo | Calentando, Falla del sensor, Sin dato, Fuera de rango, batería sin dato y aviso crítico persistente |
| Ingesta interrumpida | Tarjetas "Atrasado" con la hora de la última lectura; Robot "Estado desconocido"; Ingesta "Interrumpida" (no "apagado") |
| Teléfono sin conexión | Aviso "se muestra la captura previa del …"; al volver, la sincronización reconcilia desde su última versión |

## Pantallas

| Pantalla | Contenido | HU |
|---|---|---|
| Acceso | Etiquetas visibles, ayuda breve, errores ligados al campo, correo conservado, recuperación sin revelar si el correo existe | HU01 |
| Elegir domicilio | Selección explícita; estado "sin domicilios"; domicilio sin robot con explicación | HU01, RF02 |
| Resumen | Título, rango de datos, 2 × 2 tarjetas (4 en fila en tabletas), Operación, mapa compacto, Eventos y avisos, Conectividad, Otros canales | HU02, HU04, HU08, HU20 |
| Mapa | Plano de la versión de mapa del frame, ruta, robot con círculo de incertidumbre, estaciones, muestras de señal relativa con escala, zonas sin explorar, escala métrica, orientación, zoom y centrado | HU05, HU06 |
| Misiones | Lista paginada; detalle con trayectoria sobre la versión **exacta** de mapa, estaciones visitadas/omitidas con causa y energía con método | HU05, HU06, HU08 |
| Historial | Rango local (convertido a UTC por el caso de uso), canal, gráfica con banda mín–máx y huecos, estadísticas, tabla paginada | HU07 |
| Eventos | Filtros de periodo, severidad y estado; paginación; detalle con origen traducido, inicio, última ocurrencia y cierre | HU07, HU11 |
| Ajustes | Tema claro/oscuro/sistema, escenario de simulación, cambio de domicilio, cierre de sesión | HU12, HU15 |

El gesto atrás del sistema regresa al Resumen desde cualquier pestaña; la sincronización se detiene
en segundo plano y reconcilia al volver (RB28).

## Tokens, tipografía y accesibilidad

- **Tokens:** los 13 de la tabla 12.2 están en `ui/theme/CentinelaColors.kt`, sin hexadecimales en las
  pantallas.
- **Contraste (hallazgo):** en tema claro, `operational` (#B5D0A2) da 1.7:1 y `warning` (#FF5722) 3.2:1
  como texto sobre blanco; el texto verde "Dato válido" del prototipo no cumple el 4.5:1 que la
  propia guía exige. La propuesta dibuja cada estado como **insignia rellena** con texto oscuro
  (8.8:1 y 4.7:1) y agrega tokens derivados (`on*`, `statusNeutral`, `chrome`). Por la misma razón,
  el botón naranja "Rango de datos" y el indicador de la barra inferior usan texto #222831, no blanco.
- **Poppins y números tabulares (hallazgo):** Poppins no tiene la característica OpenType `tnum` y sus
  dígitos son proporcionales. Se incluye una variante "Poppins Centinela Tnum" (OFL 1.1, ver
  `app/OFL-Poppins.txt`) con los dígitos 0-9 de ancho fijo, para que lecturas y horas no "bailen".
- **Escala:** título 24, secciones 18-20, texto 16, lecturas 30, ayudas 13-14 (móvil). Espaciado en
  múltiplos de 4; tarjetas 12 dp, controles 8 dp, objetivos táctiles de 48 dp.
- **Íconos:** una sola familia (Material Symbols Rounded, Apache 2.0) convertida a vectores; sin
  dependencia de librerías de íconos.
- **Lectores de pantalla:** cada tarjeta es un solo elemento con descripción completa (valor, unidad,
  calidad, atraso, hora); el mapa y la gráfica tienen texto alternativo; los avisos son regiones
  vivas.
- **Sin identificadores técnicos** para el residente: no se muestran `frame_id`, `schema_version`,
  claves ni versiones de protocolo (§12.6).

## Contratos provisionales que agrega esta propuesta

Para que mapa y misiones tengan forma, `:core` gana tres piezas **provisionales** (ver ADR-005),
implementadas hoy solo por el simulador:

| Pieza | Archivo | Pendiente que resuelve |
|---|---|---|
| `FloorPlan` + `FloorPlanPort` + `QueryFloorPlan` | `homemapping/` | "Mapas" en `pendientes.md` |
| `MissionSummary` + `MissionQueryPort` + `ListMissions`/`GetMissionSummary` | `missions/` | "Misiones" en `pendientes.md` |
| `QueryChannelCapabilities` (caso de uso sobre el puerto existente) | `environmentalmonitoring/` | B8 |

Con Supabase, estos puertos son `null` y la interfaz muestra un estado explícito de "contrato
pendiente" (trayectoria sobre cuadrícula métrica, lista de misiones no disponible, unidad sin
catálogo) en lugar de inventar datos.

## Fuera de esta propuesta

Exportación CSV (HU13), push (HU16), video LAN (HU17), comandos (HU18), paneles técnicos de
calibración y auditoría (HU09 detalle, HU14) y la comparación lado a lado de misiones (HU19). La
búsqueda de la cabecera web no se trasladó a móvil porque la guía no define su comportamiento.

## Verificación hecha

- `:core` (con los contratos provisionales), el simulador, `AppContainer`, todos los ViewModels y las
  reglas de presentación compilan con Kotlin 2.4.21.
- Prueba de humo de ViewModels sobre el simulador: inicio de sesión rechazado y aceptado, selección de
  domicilio, sincronización en vivo, los cuatro escenarios, historial con agregados, eventos
  filtrados, detalle de misión antigua sobre su mapa V1 y cierre de sesión.
- `PresentationTest` (10 pruebas) y `ProvisionalContractsTest` (4 pruebas) pasan.
- Las pantallas Compose no se pudieron compilar en el entorno donde se generaron (sin acceso a Google
  Maven): se revisaron a mano y con un analizador de sintaxis. **El primer paso en Android Studio es
  sincronizar Gradle y compilar**; cualquier ajuste de API que aparezca será local a `ui/`.
