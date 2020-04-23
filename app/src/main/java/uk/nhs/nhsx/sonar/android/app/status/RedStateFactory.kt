/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

object RedStateFactory {
    private const val SEVEN_AM = "7:00:00"

    fun normal(symptomsDate: LocalDate, symptoms: Set<Symptom>): RedState {
        val suggested = sevenDaysAfter(symptomsDate)
        val tomorrow = tomorrow()

        return RedState(
            latest(
                suggested,
                tomorrow
            ).toDateTime(DateTimeZone.UTC), symptoms
        )
    }

    fun extended(vararg symptoms: Symptom): RedState =
        RedState(
            tomorrow().toDateTime(
                DateTimeZone.UTC
            ), symptoms.toSet()
        )

    private fun latest(a: DateTime, b: DateTime) =
        if (a.isAfter(b)) a else b

    private fun tomorrow() =
        LocalDate.now()
            .plusDays(1)
            .toDateTime(LocalTime.parse(SEVEN_AM))

    private fun sevenDaysAfter(symptomsDate: LocalDate) =
        symptomsDate
            .plusDays(7)
            .toDateTime(LocalTime.parse(SEVEN_AM))
}
