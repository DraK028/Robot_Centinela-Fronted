package com.robotcentinela.app.identity.domain

import java.util.UUID

/** Identificador de la persona autenticada en Supabase Auth (`auth.users.id`). */
@JvmInline
value class UserId(val value: String)

/** Identificador de un domicilio (`homes.id`, expediente §13.2). Es el límite de privacidad. */
@JvmInline
value class HomeId(val value: UUID)

/** Identificador de un robot (`robots.id`, expediente §13.2). Cada robot pertenece a un domicilio. */
@JvmInline
value class RobotId(val value: UUID)
