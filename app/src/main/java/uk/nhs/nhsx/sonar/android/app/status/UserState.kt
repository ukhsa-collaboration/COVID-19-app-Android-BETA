/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.AT_RISK
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.OK
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet

sealed class UserState {
    fun until(): DateTime? =
        when (this) {
            is DefaultState -> null
            is RecoveryState -> null
            is EmberState -> until
            is RedState -> until
            is CheckinState -> until
        }

    fun hasExpired(): Boolean =
        until()?.isBeforeNow == true

    fun displayState(): DisplayState =
        when (this) {
            is DefaultState, is RecoveryState -> OK
            is EmberState -> AT_RISK
            is RedState, is CheckinState -> ISOLATE
        }

    fun transitionOnContactAlert(): UserState? =
        when (displayState()) {
            OK -> UserStateFactory.ember()
            else -> null
        }

    fun transitionIfExpired(): UserState? =
        if (hasExpired()) DefaultState else null

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

object DefaultState : UserState()
object RecoveryState : UserState()
data class EmberState(val until: DateTime) : UserState()
data class RedState(val until: DateTime, val symptoms: NonEmptySet<Symptom>) : UserState()
data class CheckinState(val until: DateTime, val symptoms: NonEmptySet<Symptom>) : UserState()

enum class DisplayState {
    OK,
    AT_RISK,
    ISOLATE
}

enum class Symptom {
    COUGH,
    TEMPERATURE
}
