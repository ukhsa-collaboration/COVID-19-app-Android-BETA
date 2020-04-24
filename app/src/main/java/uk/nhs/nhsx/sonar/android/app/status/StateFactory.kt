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

    fun decide(symptomsDate: LocalDate, symptoms: Set<Symptom>, today: LocalDate = LocalDate.now()): UserState {
        return if (hasOnlyCough(symptoms) && isMoreThanSevenDays(symptomsDate, today)) {
            RecoveryState()
        } else {
            red(symptomsDate, symptoms, today)
        }
    }

    fun red(symptomsDate: LocalDate, symptoms: Set<Symptom>, today: LocalDate = LocalDate.now()): RedState {
        val suggested = symptomsDate.daysAfter(NO_DAYS_IN_RED - 1)
        val tomorrow = today.tomorrow()
        val redStateUntil = latest(suggested, tomorrow)

        return RedState(redStateUntil.toDateTime(UTC), symptoms)
    }

    fun extendedRed(vararg symptoms: Symptom, today: LocalDate = LocalDate.now()): RedState =
        RedState(today.tomorrow().toDateTime(UTC), symptoms.toSet())

    fun ember(today: LocalDate = LocalDate.now()): EmberState =
        EmberState(today.daysAfter(NO_DAYS_IN_EMBER - 1).toDateTime(UTC))

    private fun latest(a: DateTime, b: DateTime) =
        if (a.isAfter(b)) a else b

    private fun isMoreThanSevenDays(symptomsDate: LocalDate, today: LocalDate): Boolean {
        return !symptomsDate
            .withFixedTime()
            .plusDays(NO_DAYS_IN_RED - 1)
            .isAfter(today.withFixedTime())
    }

    private fun hasOnlyCough(symptoms: Set<Symptom>) =
        Symptom.TEMPERATURE !in symptoms

    private fun LocalDate.tomorrow() =
        daysAfter(1)

    private fun LocalDate.daysAfter(days: Int) =
        plusDays(days).withFixedTime()

    private fun LocalDate.withFixedTime() =
        this.toDateTime(LocalTime.parse(SEVEN_AM))
}
