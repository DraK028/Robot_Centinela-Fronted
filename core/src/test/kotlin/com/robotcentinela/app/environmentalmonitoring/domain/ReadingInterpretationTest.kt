package com.robotcentinela.app.environmentalmonitoring.domain

import com.robotcentinela.app.testing.TestFrames.reading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** HU02, HU20, RB04, RB05, RB09, casos T11 y T12 del expediente. */
class ReadingInterpretationTest {

    private fun capability(key: String, unit: String?) =
        ChannelCapability(ChannelKey(key), "metric", unit, null, null, enabled = true)

    @Test
    fun `sensor no instalado conserva su estado propio y no muestra cero`() {
        val result = ReadingInterpreter.interpret(reading("mq7_raw", null, ReadingQuality.NotInstalled))
        assertEquals(ReadingInterpretation.NotInstalled, result)
    }

    @Test
    fun `MQ sin calibrar se muestra como senal cruda`() {
        val result = ReadingInterpreter.interpret(
            reading("mq6_raw", 1860.0, ReadingQuality.Uncalibrated),
            capability("mq6_raw", "adc_count"),
        )
        assertEquals(ReadingInterpretation.RawSignal(ReadingValue.Numeric(1860.0), "adc_count"), result)
    }

    @Test
    fun `senal sin calibrar nunca lleva unidad de concentracion`() {
        val result = ReadingInterpreter.interpret(
            reading("mq6_raw", 1860.0, ReadingQuality.Uncalibrated),
            capability("mq6_raw", "ppm"),
        )
        val raw = assertIs<ReadingInterpretation.RawSignal>(result)
        assertNull(raw.unitCode)
    }

    @Test
    fun `valor nulo con calidad valida es sin dato y nunca cero`() {
        val result = ReadingInterpreter.interpret(reading("temperature", null, ReadingQuality.Valid))
        assertEquals(ReadingInterpretation.NoData, result)
    }

    @Test
    fun `fallo calentamiento y fuera de rango son estados distintos aunque traigan valor`() {
        assertEquals(
            ReadingInterpretation.Warming,
            ReadingInterpreter.interpret(reading("mq6_raw", 300.0, ReadingQuality.Warming)),
        )
        assertEquals(
            ReadingInterpretation.Fault,
            ReadingInterpreter.interpret(reading("mq6_raw", 0.0, ReadingQuality.Fault)),
        )
        assertEquals(
            ReadingInterpretation.OutOfRange,
            ReadingInterpreter.interpret(reading("mq6_raw", 4095.0, ReadingQuality.OutOfRange)),
        )
    }

    @Test
    fun `lectura valida usa la unidad del catalogo`() {
        val result = ReadingInterpreter.interpret(
            reading("temperature", 24.6),
            capability("temperature", "celsius"),
        )
        assertEquals(
            ReadingInterpretation.Measured(ReadingValue.Numeric(24.6), "celsius", ReadingQuality.Valid),
            result,
        )
    }

    @Test
    fun `sin catalogo la unidad queda no disponible`() {
        val result = assertIs<ReadingInterpretation.Measured>(
            ReadingInterpreter.interpret(reading("humidity", 48.1)),
        )
        assertNull(result.unitCode)
    }

    @Test
    fun `lectura booleana valida conserva su valor logico`() {
        val ir = SensorReading(
            ChannelKey("ir_floor_front_left"),
            ReadingValue.BooleanValue(true),
            ReadingQuality.Valid,
            null,
        )
        val result = assertIs<ReadingInterpretation.Measured>(ReadingInterpreter.interpret(ir))
        assertEquals(ReadingValue.BooleanValue(true), result.value)
    }

    @Test
    fun `calidad desconocida no se afirma como valida`() {
        val result = ReadingInterpreter.interpret(reading("temperature", 20.0, ReadingQuality.fromRaw("sospechosa")))
        assertEquals(ReadingInterpretation.Uncertain(ReadingValue.Numeric(20.0), "sospechosa"), result)
    }

    @Test
    fun `catalogo de calidad del glosario`() {
        val catalog = listOf("valid", "estimated", "uncalibrated", "warming", "fault", "out_of_range", "not_installed")
        catalog.forEach { raw ->
            val quality = ReadingQuality.fromRaw(raw)
            assertEquals(raw, quality.raw)
            check(quality !is ReadingQuality.Unknown) { "$raw debería estar en el catálogo" }
        }
        assertIs<ReadingQuality.Unknown>(ReadingQuality.fromRaw("VALID"))
    }
}
