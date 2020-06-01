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
import uk.nhs.nhsx.sonar.android.app.util.latest
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized

sealed class UserState {

    companion object {
        const val NO_DAYS_IN_SYMPTOMATIC = 7
        const val NO_DAYS_IN_EXPOSED = 14

        fun default(): DefaultState =
            DefaultState

        fun exposed(exposureDate: LocalDate): ExposedState =
            ExposedState(
                exposureDate.toUtcNormalized(),
                exposureDate.plusDays(NO_DAYS_IN_EXPOSED - 1).toUtcNormalized()
            )

        fun exposed(state: ExposedSymptomaticState): ExposedState =
            ExposedState(state.since, state.until)

        fun symptomatic(
            symptomsDate: LocalDate,
            symptoms: NonEmptySet<Symptom>,
            today: LocalDate = LocalDate.now()
        ): SymptomaticState {
            val suggested = symptomsDate.plusDays(NO_DAYS_IN_SYMPTOMATIC)
            val tomorrow = today.plusDays(1)

            // if symptomsDate > 7 days ago then symptomatic state is until tomorrow
            // if symptomsDate <= 7 days ago then symptomatic state is until suggested
            val until = latest(suggested, tomorrow)

            return SymptomaticState(
                symptomsDate.toUtcNormalized(),
                until.toUtcNormalized(),
                symptoms
            )
        }

        fun exposedSymptomatic(
            symptomsDate: LocalDate,
            state: ExposedState,
            symptoms: NonEmptySet<Symptom>
        ): ExposedSymptomaticState =
            ExposedSymptomaticState(symptomsDate.toUtcNormalized(), state.until, symptoms)

        fun positive(
            testDate: DateTime,
            today: LocalDate = LocalDate.now()
        ): PositiveState {
            val testLocalDate = testDate.toLocalDate()
            val suggested = testLocalDate.plusDays(NO_DAYS_IN_SYMPTOMATIC)
            val tomorrow = today.plusDays(1)

            // if testDate > 7 days ago then positive state is until tomorrow
            // if testDate <= 7 days ago then positive state is until suggested
            val until = latest(suggested, tomorrow)

            return PositiveState(
                testLocalDate.toUtcNormalized(),
                until.toUtcNormalized(),
                emptySet()
            )
        }

        fun positive(
            testDate: DateTime,
            state: SymptomaticState
        ): PositiveState = PositiveState(
                testDate.toLocalDate().toUtcNormalized(),
                state.until,
                state.symptoms
        )

        fun positive(
            testDate: DateTime,
            state: ExposedSymptomaticState
        ): PositiveState = PositiveState(
            testDate.toLocalDate().toUtcNormalized(),
            state.until,
            state.symptoms
        )
    }

    fun until(): DateTime? =
        when (this) {
            is ExposedState -> until
            is SymptomaticState -> until
            is ExposedSymptomaticState -> until
            is PositiveState -> until
            is DefaultState -> null
        }

    fun expire(today: LocalDate = LocalDate.now()): UserState =
        when (this) {
            is ExposedState -> this.copy(until = today.minusDays(1).toUtcNormalized())
            is SymptomaticState -> this.copy(until = today.minusDays(1).toUtcNormalized())
            is ExposedSymptomaticState -> this.copy(until = today.minusDays(1).toUtcNormalized())
            is PositiveState -> this.copy(until = today.minusDays(1).toUtcNormalized())
            is DefaultState -> DefaultState
        }

    fun hasExpired(): Boolean =
        until()?.isBeforeNow == true

    fun displayState(): DisplayState =
        when (this) {
            is DefaultState -> OK
            is ExposedState -> AT_RISK
            is SymptomaticState -> ISOLATE
            is ExposedSymptomaticState -> ISOLATE
            is PositiveState -> ISOLATE
        }

    fun extend(symptoms: Set<Symptom>, today: LocalDate = LocalDate.now()): UserState =
        when (this) {
            is PositiveState -> this.copy(
                symptoms = symptoms,
                until = today.plusDays(1).toUtcNormalized()
            )
            is SymptomaticState -> this.copy(
                symptoms = NonEmptySet.create(symptoms)!!,
                until = today.plusDays(1).toUtcNormalized()
            )
            is ExposedSymptomaticState -> this.copy(
                symptoms = NonEmptySet.create(symptoms)!!,
                until = today.plusDays(1).toUtcNormalized()
            )
            is ExposedState -> this
            is DefaultState -> this
        }

    fun scheduleCheckInReminder(reminders: Reminders) {
        reminders.cancelCheckinReminder()

        if (hasExpired()) return

        return when (this) {
            is SymptomaticState -> reminders.scheduleCheckInReminder(until)
            is ExposedSymptomaticState -> reminders.scheduleCheckInReminder(until)
            is PositiveState -> reminders.scheduleCheckInReminder(until)
            else -> Unit
        }
    }

    fun symptoms(): Set<Symptom> =
        when (this) {
            is SymptomaticState -> symptoms
            is PositiveState -> symptoms
            else -> emptySet()
        }
}

// Initial state
object DefaultState : UserState() {
    override fun toString(): String = "DefaultState"
}

// State when you have been in contact with someone in SymptomaticState
data class ExposedState(
    val since: DateTime,
    val until: DateTime
) : UserState()

// State when you initially have symptoms. Prompted after 1 to 7 days to checkin.
data class SymptomaticState(
    val since: DateTime,
    val until: DateTime,
    val symptoms: NonEmptySet<Symptom>
) : UserState()

// State when you initially are exposed, then later become symptomatic.
data class ExposedSymptomaticState(
    val since: DateTime,
    val until: DateTime,
    val symptoms: NonEmptySet<Symptom>
) : UserState()

// State when user has tested and the test result was positive
data class PositiveState(
    val since: DateTime,
    val until: DateTime,
    val symptoms: Set<Symptom> = emptySet()
) : UserState()

enum class DisplayState {
    OK, // Default
    AT_RISK, // Exposed
    ISOLATE // Symptomatic
}

enum class Symptom(val value: String) {
    COUGH("COUGH"),
    TEMPERATURE("TEMPERATURE"),
    ANOSMIA("ANOSMIA"),
    SNEEZE("SNEEZE"),
    NAUSEA("NAUSEA");

    companion object {
        fun fromValue(value: String) = values().firstOrNull { it.value == value }
    }
}
