package com.robotcentinela.app.shared

/**
 * Errores que los puertos y casos de uso exponen al resto de la app.
 *
 * Los adaptadores traducen las excepciones de su infraestructura (Supabase, Ktor, etc.) a estos
 * tipos, de modo que el dominio y la futura UI no dependan de un SDK concreto (RNF18).
 */
sealed class CentinelaException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Sin sesión válida, sesión vencida o rechazo de permisos del servidor (RF01, RF02).
 * La autorización real la aplica el servidor (RLS); este error solo la comunica.
 */
class AccessDeniedException(message: String, cause: Throwable? = null) : CentinelaException(message, cause)

/**
 * El recurso pedido no pertenece al ámbito de domicilio seleccionado (RF02, HU01 CDA 3,
 * HU07 CDA 2). Es una comprobación del cliente; no sustituye la protección del servidor (RNF09).
 */
class NotInScopeException(message: String) : CentinelaException(message)

/** Fallo de red o del servicio remoto. No implica que el robot esté apagado (RF29). */
class DataAccessException(message: String, cause: Throwable? = null) : CentinelaException(message, cause)

/** La respuesta del servidor no cumple el contrato de datos esperado. */
class ContractViolationException(message: String, cause: Throwable? = null) : CentinelaException(message, cause)
