/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet

sealed class UserState {
    abstract val until: DateTime

    fun hasExpired(): Boolean =
        until.isBeforeNow

    fun isOk(): Boolean =
        this is DefaultState || this is RecoveryState

    fun isAtRisk(): Boolean =
        this is EmberState

    fun shouldIsolate(): Boolean =
        this is RedState || this is CheckinState

    fun transitionOnContactAlert(): UserState? =
        if (isOk()) UserStateFactory.ember() else null

    fun transitionIfExpired(): UserState? =
        if (hasExpired()) DefaultState() else null

    fun scheduleCheckInReminder(reminders: Reminders) =
        when {
            (this is RedState && !hasExpired()) -> reminders.scheduleCheckInReminder(until)
            else -> Unit
        }

    fun symptoms(): Set<Symptom> =
        when (this) {
            is RedState -> symptoms
            is CheckinState -> symptoms
            else -> emptySet()
        }
}

data class DefaultState(override val until: DateTime = DateTime.now(UTC)) : UserState()
data class RecoveryState(override val until: DateTime = DateTime.now(UTC)) : UserState()
data class EmberState(override val until: DateTime) : UserState()
data class RedState(override val until: DateTime, val symptoms: NonEmptySet<Symptom>) : UserState()
data class CheckinState(override val until: DateTime, val symptoms: NonEmptySet<Symptom>) : UserState()

enum class Symptom {
    COUGH,
    TEMPERATURE
}
