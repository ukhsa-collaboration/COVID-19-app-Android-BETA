/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet

object UserStateFactory {
    private const val SEVEN_AM = "7:00:00"
    private const val NO_DAYS_IN_RED = 7
    private const val NO_DAYS_IN_AMBER = 14

    fun questionnaire(
        symptomsDate: LocalDate,
        symptoms: NonEmptySet<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState =
        if (doesNotHaveTemperature(symptoms) && isMoreThanSevenDays(symptomsDate, today)) {
            RecoveryState
        } else {
            buildRed(symptomsDate, symptoms, today)
        }

    fun checkinQuestionnaire(
        symptoms: Set<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState =
        when {
            hasTemperature(symptoms) -> buildCheckin(NonEmptySet.create(symptoms)!!, today)
            hasCough(symptoms) -> RecoveryState
            else -> DefaultState
        }

    fun buildAmber(today: LocalDate = LocalDate.now()): AmberState =
        AmberState(today.after(NO_DAYS_IN_AMBER - 1).days().toUtc())

    private fun buildCheckin(symptoms: NonEmptySet<Symptom>, today: LocalDate = LocalDate.now()): CheckinState =
        CheckinState(today.after(1).day().toUtc(), symptoms)

    private fun buildRed(
        symptomsDate: LocalDate,
        symptoms: NonEmptySet<Symptom>,
        today: LocalDate = LocalDate.now()
    ): RedState {
        val suggested = symptomsDate.after(NO_DAYS_IN_RED).days()
        val tomorrow = today.after(1).day()
        val redStateUntil = latest(suggested, tomorrow)

        return RedState(redStateUntil.toUtc(), symptoms)
    }

    private fun latest(a: DateTime, b: DateTime) =
        if (a.isAfter(b)) a else b

    private fun isMoreThanSevenDays(symptomsDate: LocalDate, today: LocalDate): Boolean =
        !symptomsDate
            .atSevenAm()
            .plusDays(NO_DAYS_IN_RED)
            .isAfter(today.atSevenAm())

    private fun doesNotHaveTemperature(symptoms: Set<Symptom>): Boolean =
        !hasTemperature(symptoms)

    private fun hasTemperature(symptoms: Set<Symptom>): Boolean =
        Symptom.TEMPERATURE in symptoms

    private fun hasCough(symptoms: Set<Symptom>): Boolean =
        Symptom.COUGH in symptoms

    private fun LocalDate.atSevenAm(): DateTime =
        toDateTime(LocalTime.parse(SEVEN_AM))

    private fun LocalDate.after(count: Int): After =
        After(this, count)

    private fun DateTime.toUtc(): DateTime =
        toDateTime(UTC)

    private class After(val date: LocalDate, val count: Int) {
        fun days(): DateTime =
            date.plusDays(count).atSevenAm()

        fun day() = days()
    }
}
