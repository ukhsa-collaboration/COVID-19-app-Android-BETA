/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.joda.time.LocalTime

object RedStateFactory {
    private const val SEVEN_AM = "7:00:00"

    fun normal(symptomsDate: LocalDate, symptoms: Set<Symptom>, today: LocalDate = LocalDate.now()): RedState {
        val suggested = sixDaysAfter(symptomsDate)
        val tomorrow = tomorrow(today)
        val redStateUntil = latest(suggested, tomorrow)

        return RedState(redStateUntil.toDateTime(UTC), symptoms)
    }

    fun extended(vararg symptoms: Symptom, today: LocalDate = LocalDate.now()): RedState =
        RedState(
            tomorrow(today).toDateTime(UTC), symptoms.toSet()
        )

    private fun latest(a: DateTime, b: DateTime) =
        if (a.isAfter(b)) a else b

    private fun tomorrow(today: LocalDate) =
        today
            .plusDays(1)
            .toDateTime(LocalTime.parse(SEVEN_AM))

    private fun sixDaysAfter(symptomsDate: LocalDate) =
        symptomsDate
            .plusDays(6)
            .toDateTime(LocalTime.parse(SEVEN_AM))
}
