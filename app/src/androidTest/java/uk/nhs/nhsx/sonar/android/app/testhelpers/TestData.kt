/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.ExposedSymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class TestData {

    val today: LocalDate = LocalDate.now()
    val yesterday: LocalDate = today.minusDays(1)
    private val tomorrow: LocalDate = today.plusDays(1)

    val expiredSymptomaticState = SymptomaticState(
        since = DateTime.now(DateTimeZone.UTC).minusSeconds(1),
        until = DateTime.now(DateTimeZone.UTC).minusSeconds(1),
        symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
    )

    val defaultState: UserState = UserState.default()

    val exposedState = UserState.exposed(
        exposureDate = today
    )

    val expiredExposedState = UserState.exposed(
        exposureDate = today.minusDays(15)
    )

    val symptomaticState = SymptomaticState(
        since = DateTime.now(DateTimeZone.UTC).minusDays(1),
        until = DateTime.now(DateTimeZone.UTC).plusDays(1),
        symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
    )

    val exposedSymptomaticState = UserState.exposedSymptomatic(
        symptomsDate = yesterday,
        state = exposedState,
        symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
    )

    val positiveState = PositiveState(
        since = DateTime.now(DateTimeZone.UTC).minusDays(1),
        until = DateTime.now(DateTimeZone.UTC).plusDays(1),
        symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
    )

    val expiredPositiveState = PositiveState(
        since = DateTime.now(DateTimeZone.UTC).minusDays(15),
        until = DateTime.now(DateTimeZone.UTC).minusDays(1),
        symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
    )

    private fun exposedToday(): ExposedState =
        UserState.exposed(
            exposureDate = today
        )

    fun exposedYesterday(): ExposedState =
        UserState.exposed(
            exposureDate = yesterday
        )

    fun symptomaticYesterday(): SymptomaticState =
        UserState.symptomatic(
            symptomsDate = yesterday,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

    fun positiveToday(): PositiveState =
        UserState.positive(
            testDate = today.toDateTime(LocalTime.now())
        )

    fun exposedSymptomaticYesterday() =
        UserState.exposedSymptomatic(
            symptomsDate = yesterday,
            state = exposedYesterday(),
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

    fun symptomaticTomorrow(): SymptomaticState =
        UserState.symptomatic(
            symptomsDate = tomorrow,
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )

    fun exposedSymptomaticToday(): ExposedSymptomaticState =
        UserState.exposedSymptomatic(
            symptomsDate = today,
            state = exposedToday(),
            symptoms = nonEmptySetOf(Symptom.TEMPERATURE)
        )
}
