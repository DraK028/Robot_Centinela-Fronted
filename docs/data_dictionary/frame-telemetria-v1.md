# Diccionario de datos: frame de telemetría v1

**Fuente del contrato:** `JSON_estructura.txt` (`schema_version` 1.1, ejemplo del 11 de octubre de
2026), junto con las reglas semánticas del expediente técnico (§7.2, §13.2, §13.3).
**Fixture idéntico:** `app/src/test/resources/fixtures/telemetry/frame_v1_1_device_payload.json`.
**DTO:** `TelemetryFrameRowDto` · **Mapeador:** `TelemetryFrameMapper` · **Dominio:** `TelemetryFrame`.

## Campos del dispositivo

| Campo JSON | Tipo | Nulo | Dominio | Regla que aplica la app |
|---|---|---|---|---|
| `schema_version` | string | No | `schemaVersion` | Solo se acepta la versión mayor `1`. Otra versión rechaza el frame (§14.2) |
| `event_id` | UUID | No | `FrameId` | Identidad inmutable del frame; misma en web y móvil (RF06, RB02) |
| `device_id` | UUID | No | `DeviceId` | Dispositivo que capturó. No autoriza nada: el domicilio lo resuelve el servidor (RB01) |
| `boot_id` | UUID | No | `BootId` | Cambia en cada reinicio |
| `sequence` | entero ≥ 0 | No | `sequence` | Orden de captura dentro del boot. Decide el dato más reciente dentro del mismo boot |
| `captured_at` | RFC3339 UTC | Sí | `capturedAt` | Hora de captura del dispositivo. El cliente nunca la inventa. Solo es confiable con `clock_quality = synced` |
| `monotonic_ms` | entero ≥ 0 | No | `monotonicMs` | Tiempo relativo del dispositivo |
| `clock_quality` | string | No | `ClockQuality` | `synced` / `unsynced`; otro valor se trata como no confiable |
| `mission_id` | UUID | Sí | `MissionId` | `null` cuando no hay misión (§7.2) |
| `map_version_id` | UUID | Sí | `MapVersionId` | Versión exacta del mapa de la captura (HU05, RB13) |
| `pose.x_m`, `pose.y_m` | número (m) | — | `Pose` | Posición estimada, no medida (RNF12) |
| `pose.theta_rad` | número (rad) | — | `Pose` | Orientación |
| `pose.sigma_position_m` | número ≥ 0 (m) | Sí | `Pose.sigmaPositionM` | Incertidumbre. `null` = desconocida, nunca cero (HU06) |
| `pose.quality` | string | — | `Pose.quality` | Se conserva como texto (p. ej. `estimated`); el catálogo no está documentado |
| `pose` (objeto completo) | objeto | Sí | `pose` | Sin pose no hay posición: nunca se dibuja en el origen (RF12) |
| `readings[]` | lista | No | `readings` | Un `channel_key` por frame (§7.2) |
| `readings[].channel_key` | string | No | `ChannelKey` | El significado y la unidad vienen del registro del dispositivo, no del frame |
| `readings[].value` | número | Sí | `ReadingValue.Numeric` | Excluyente con `boolean_value` |
| `readings[].boolean_value` | booleano | Sí | `ReadingValue.BooleanValue` | Excluyente con `value`. `null` **nunca** se convierte en `false` (§13.3) |
| `readings[].quality` | string | No | `ReadingQuality` | Catálogo del glosario §19.2 (ver abajo) |
| `readings[].calibration_id` | UUID | Sí | `CalibrationId` | Calibración aplicada. El histórico es inmutable (RB07) |
| `battery.voltage_v` | número ≥ 0 (V) | Sí | `BatteryStatus.voltageV` | Tensión |
| `battery.soc_pct` | número 0 a 100 (%) | Sí | `BatteryStatus.socPct` | `null` produce "Porcentaje no disponible" (HU08). La tensión nunca se convierte linealmente |
| `battery.current_a` | número (A) | Sí | `BatteryStatus.currentA` | Corriente, si se mide |
| `battery.quality` | string | — | `BatteryStatus.quality` | Método declarado (p. ej. `voltage_only`) |
| `robot_state` | string | Sí | `RobotReportedState` | Estado reportado por el dispositivo, como texto original (RF19) |
| `motors_active` | booleano | Sí | `motorsActive` | Orden o estado reportado; no prueba giro efectivo (§7.0) |
| `firmware_version` | string | Sí | `firmwareVersion` | Versión del firmware |

## Canales presentes en `JSON_estructura.txt`

| `channel_key` | Tipo de valor | Calidad en el ejemplo | Observación |
|---|---|---|---|
| `mq6_raw` | numérico | `uncalibrated` | Señal cruda del MQ6. **No es ppm** (RB05) |
| `mq4_raw` | numérico | `uncalibrated` | Señal cruda del MQ4. **No es ppm** |
| `mq7_raw` | numérico (`null`) | `not_installed` | Sensor no instalado: estado propio, nunca cero |
| `temperature` | numérico | `valid` | La unidad la define el catálogo de capacidades |
| `humidity` | numérico | `valid` | Porcentaje registrado (§13.3) |
| `front_distance_m` | numérico | `valid` | Metros. Un timeout no se guarda como 0 (§7.2) |
| `ir_floor_front_left` / `_right` | booleano | `valid` | IR de piso. `true` = piso detectado, `false` = riesgo de borde (§7.2) |
| `ir_floor_rear_left` / `_right` | booleano | `valid` | Igual que los anteriores |

La app no tiene una lista fija de canales: acepta cualquier `channel_key` y obtiene su semántica del
catálogo de capacidades (RF05, pendiente).

## Catálogo de calidad de lectura (glosario §19.2)

| `quality` | Interpretación en la app (`ReadingInterpretation`) |
|---|---|
| `valid` | `Measured`, con la unidad del catálogo |
| `estimated` | `Measured`, con la unidad del catálogo |
| `uncalibrated` | `RawSignal`: nunca lleva unidad de concentración (`ppm`) |
| `warming` | `Warming`: no se muestra concentración |
| `fault` | `Fault` |
| `out_of_range` | `OutOfRange` |
| `not_installed` | `NotInstalled` |
| otro valor | `Uncertain`: se conserva el valor, pero no se afirma su validez |
| (valor `null` con calidad válida, estimada o sin calibrar) | `NoData` ("Sin dato"), nunca 0 (RB04, RB09) |

## Campos asignados por el backend (PROVISIONALES)

No forman parte del payload del dispositivo. El backend los agrega al persistir (§7.2: "El backend
asigna received_at y stream_version"). Mientras backend no publique su contrato, la app espera que
el recurso `telemetry_frames_v1` los entregue al nivel superior de cada fila:

| Campo | Tipo | Uso en la app |
|---|---|---|
| `robot_id` | UUID | Filtro de consulta y de Realtime |
| `received_at` | RFC3339 | Ingesta interrumpida tras 10 s sin datos (RNF04) y orden aproximado si el reloj no es confiable. Si falta, el frame se rechaza |
| `stream_version` | entero ≥ 0 | Cursor de sincronización (§7.4). Si falta, la fila incumple el contrato |

## Compatibilidad

- **Cambios aditivos** (campos nuevos): se ignoran (`ignoreUnknownKeys`) y siguen en v1.
- **Cambios de significado o eliminaciones**: requieren v2 con un adaptador nuevo (§14.2).
- **Diferencias con el ejemplo del expediente** (§7.2, `schema_version` 1.0): el contrato vigente
  (1.1) agrega `mq4_raw`, `mq7_raw`, `front_distance_m`, los cuatro IR de piso con `boolean_value` y
  `motors_active`. El expediente usaba el canal `methane_estimate`, que no aparece en 1.1.
