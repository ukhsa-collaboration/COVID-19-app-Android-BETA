/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime

sealed class UserState {
    abstract val until: DateTime

    fun hasExpired(): Boolean = until.isBeforeNow
}

data class DefaultState(override val until: DateTime) : UserState()

data class RecoveryState(override val until: DateTime) : UserState()

data class EmberState(override val until: DateTime) : UserState()

data class RedState(override val until: DateTime, val symptoms: Set<Symptom>) : UserState() {
    companion object {
        private const val SEVEN_AM = "7:00:00"

        fun normal(symptomsDate: LocalDate, symptoms: Set<Symptom>): RedState {
            val suggested = sevenDaysAfter(symptomsDate)
            val tomorrow = tomorrow()

            return RedState(latest(suggested, tomorrow).toDateTime(UTC), symptoms)
        }

        fun extended(vararg symptoms: Symptom): RedState =
            RedState(tomorrow().toDateTime(UTC), symptoms.toSet())

        private fun latest(a: LocalDateTime, b: LocalDateTime) =
            if (a.isAfter(b)) a else b

        private fun tomorrow() =
            LocalDate.now()
                .plusDays(1)
                .toLocalDateTime(LocalTime.parse(SEVEN_AM))

        private fun sevenDaysAfter(symptomsDate: LocalDate) =
            symptomsDate
                .plusDays(7)
                .toLocalDateTime(LocalTime.parse(SEVEN_AM))
    }
}

enum class Symptom {
    COUGH,
    TEMPERATURE
}
