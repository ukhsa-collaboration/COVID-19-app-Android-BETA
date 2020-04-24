/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

sealed class UserState {
    abstract val until: DateTime

    fun hasExpired(): Boolean = until.isBeforeNow
}

data class DefaultState(override val until: DateTime = DateTime.now(UTC)) : UserState()

data class RecoveryState(override val until: DateTime = DateTime.now(UTC)) : UserState()

data class EmberState(override val until: DateTime) : UserState()

data class RedState(override val until: DateTime, val symptoms: Set<Symptom>) : UserState()

enum class Symptom {
    COUGH,
    TEMPERATURE
}
