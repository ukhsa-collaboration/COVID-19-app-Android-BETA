/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.AT_RISK
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.OK
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.after
import uk.nhs.nhsx.sonar.android.app.util.latest
import uk.nhs.nhsx.sonar.android.app.util.toUtc

sealed class UserState {

    companion object {
        const val NO_DAYS_IN_RED = 7
        const val NO_DAYS_IN_AMBER = 14

        fun amber(today: LocalDate = LocalDate.now()): AmberState =
            AmberState(today.after(NO_DAYS_IN_AMBER - 1).days().toUtc())

        fun checkin(symptoms: NonEmptySet<Symptom>, today: LocalDate = LocalDate.now()): CheckinState =
            CheckinState(today.after(1).day().toUtc(), symptoms)

        fun red(
            symptomsDate: LocalDate,
            symptoms: NonEmptySet<Symptom>,
            today: LocalDate = LocalDate.now()
        ): RedState {
            val suggested = symptomsDate.after(NO_DAYS_IN_RED).days()
            val tomorrow = today.after(1).day()
            val redStateUntil = latest(suggested, tomorrow)

            return RedState(redStateUntil.toUtc(), symptoms)
        }
    }

    fun until(): DateTime? =
        when (this) {
            is DefaultState -> null
            is RecoveryState -> null
            is AmberState -> until
            is RedState -> until
            is CheckinState -> until
        }

    fun hasExpired(): Boolean =
        until()?.isBeforeNow == true

    fun displayState(): DisplayState =
        when (this) {
            is DefaultState, is RecoveryState -> OK
            is AmberState -> AT_RISK
            is RedState, is CheckinState -> ISOLATE
        }

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

// Initial state
object DefaultState : UserState()

// State when you had symptoms and now you only have cough after more than seven days.
object RecoveryState : UserState()

// State when you have been in contact with someone in RedState
data class AmberState(val until: DateTime) : UserState()

// State when you initially have symptoms. Prompted after 1 to 7 days to checkin.
data class RedState(val until: DateTime, val symptoms: NonEmptySet<Symptom>) : UserState()

// State after first checkin from RedState, does not get prompted again.
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
