/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.AT_RISK
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.OK
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.after
import uk.nhs.nhsx.sonar.android.app.util.latest
import uk.nhs.nhsx.sonar.android.app.util.toUtc

sealed class UserState {

    abstract val testInfo: TestInfo?

    companion object {
        const val NO_DAYS_IN_RED = 7
        const val NO_DAYS_IN_AMBER = 14

        fun default(testInfo: TestInfo?): DefaultState =
            DefaultState(testInfo)

        fun amber(today: LocalDate = LocalDate.now()): AmberState =
            AmberState(today.after(NO_DAYS_IN_AMBER - 1).days().toUtc())

        fun checkin(
            symptomsDate: DateTime?,
            symptoms: NonEmptySet<Symptom>,
            today: LocalDate = LocalDate.now()
        ): CheckinState =
            CheckinState(
                today.after(1).day().toUtc(),
                symptoms,
                symptomsDate
            )

        fun red(
            symptomsDate: LocalDate,
            symptoms: NonEmptySet<Symptom>,
            today: LocalDate = LocalDate.now()
        ): RedState {
            val suggested = symptomsDate.after(NO_DAYS_IN_RED).days()
            val tomorrow = today.after(1).day()

            // if symptomsDate > 7 days ago then red state is until tomorrow
            // if symptomsDate <= 7 days ago then red state is until suggested
            val redStateUntil = latest(suggested, tomorrow)

            return RedState(
                redStateUntil.toUtc(),
                symptoms,
                symptomsDate.toDateTime(LocalTime(UTC))
            )
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
            is DefaultState -> OK
            is RecoveryState -> OK
            is AmberState -> AT_RISK
            is RedState -> ISOLATE
            is CheckinState -> ISOLATE
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

    fun displayTestResult(): Boolean =
        testInfo != null && !testInfo!!.dismissed

    fun symptomsStartDate(): DateTime? =
        when (this) {
            is RedState -> symptomsStartDate
            is CheckinState -> symptomsStartDate
            else -> null
        }
}

// Initial state
data class DefaultState(
    override var testInfo: TestInfo? = null
) : UserState()

// State when you had symptoms and now you only have cough after more than seven days.
data class RecoveryState(
    override var testInfo: TestInfo? = null
) : UserState()

// State when you have been in contact with someone in RedState
data class AmberState(
    val until: DateTime,
    override var testInfo: TestInfo? = null
) : UserState()

// State when you initially have symptoms. Prompted after 1 to 7 days to checkin.
data class RedState(
    val until: DateTime,
    val symptoms: NonEmptySet<Symptom>,
    val symptomsStartDate: DateTime? = null,
    override var testInfo: TestInfo? = null
) : UserState()

// State after first checkin from RedState, does not get prompted again.
data class CheckinState(
    val until: DateTime,
    val symptoms: NonEmptySet<Symptom>,
    val symptomsStartDate: DateTime? = null,
    override var testInfo: TestInfo? = null
) : UserState()

// Test Info
data class TestInfo(
    val testResult: TestResult,
    val testDate: DateTime,
    val stateChanged: Boolean,
    var dismissed: Boolean = false
)

enum class TestResult {
    NEGATIVE,
    POSITIVE,
    INVALID,
    PRESUMED_POSTIVE
}

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
