/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime

sealed class UserState {

    abstract val until: DateTime

    fun hasExpired(): Boolean =
        until.isBeforeNow

    override fun toString(): String =
        "UserState(${javaClass.simpleName})"
}

data class DefaultState(override val until: DateTime) : UserState()
data class EmberState(override val until: DateTime) : UserState()
data class RedState(override val until: DateTime, val symptoms: Set<Symptom>) : UserState()

enum class Symptom {
    COUGH,
    TEMPERATURE
}
