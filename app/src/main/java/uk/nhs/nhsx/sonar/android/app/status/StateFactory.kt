/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.joda.time.LocalTime

object StateFactory {
    private const val SEVEN_AM = "7:00:00"
    private const val NO_DAYS_IN_RED = 7
    private const val NO_DAYS_IN_EMBER = 14

    fun red(symptomsDate: LocalDate, symptoms: Set<Symptom>, today: LocalDate = LocalDate.now()): RedState {
        val suggested = daysAfter(symptomsDate, NO_DAYS_IN_RED - 1)
        val tomorrow = tomorrow(today)
        val redStateUntil = latest(suggested, tomorrow)

        return RedState(redStateUntil.toDateTime(UTC), symptoms)
    }

    fun extendedRed(vararg symptoms: Symptom, today: LocalDate = LocalDate.now()): RedState =
        RedState(tomorrow(today).toDateTime(UTC), symptoms.toSet())

    fun ember(today: LocalDate = LocalDate.now()): EmberState =
        EmberState(daysAfter(today, NO_DAYS_IN_EMBER - 1).toDateTime(UTC))

    private fun latest(a: DateTime, b: DateTime) =
        if (a.isAfter(b)) a else b

    private fun tomorrow(today: LocalDate) =
        daysAfter(today, 1)

    private fun daysAfter(symptomsDate: LocalDate, days: Int) =
        symptomsDate
            .plusDays(days)
            .toDateTime(LocalTime.parse(SEVEN_AM))
}
