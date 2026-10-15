package com.robotcentinela.app.shell.supabase

import com.robotcentinela.app.shared.AccessDeniedException
import com.robotcentinela.app.shared.CentinelaException
import com.robotcentinela.app.shared.ContractViolationException
import com.robotcentinela.app.shared.DataAccessException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.time.DateTimeException

/**
 * Ejecuta una llamada a Supabase y traduce sus excepciones a los errores de `:core`, para que el
 * dominio no dependa del SDK (RNF18).
 *
 * Los mensajes no incluyen cuerpos de respuesta ni payloads (RNF19).
 */
internal suspend inline fun <T> supabaseCall(operation: String, block: () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CentinelaException) {
        throw e
    } catch (e: RestException) {
        when (e.statusCode) {
            401, 403 -> throw AccessDeniedException("$operation: acceso denegado (${e.statusCode})", e)
            else -> throw DataAccessException("$operation: el servidor respondió ${e.statusCode}", e)
        }
    } catch (e: HttpRequestException) {
        throw DataAccessException("$operation: error de red", e)
    } catch (e: SerializationException) {
        throw ContractViolationException("$operation: la respuesta no cumple el contrato esperado", e)
    } catch (e: IllegalArgumentException) {
        throw ContractViolationException("$operation: un valor no cumple el contrato esperado", e)
    } catch (e: DateTimeException) {
        throw ContractViolationException("$operation: una fecha no cumple el contrato esperado", e)
    }
