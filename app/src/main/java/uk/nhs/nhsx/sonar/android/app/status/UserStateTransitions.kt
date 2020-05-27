/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.Symptom.ANOSMIA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_SYMPTOMATIC
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.positive
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.isEarlierThan

object UserStateTransitions {

    fun diagnose(
        currentState: UserState,
        symptomsDate: LocalDate,
        symptoms: NonEmptySet<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState {
        val isInSevenDayWindow = !symptomsDate.isEarlierThan(NO_DAYS_IN_SYMPTOMATIC, today)

        return when (hasTemperature(symptoms) || isInSevenDayWindow) {
            true -> UserState.symptomatic(symptomsDate, symptoms, today)
            else -> currentState
        }
    }

    fun diagnoseForCheckin(
        currentState: UserState,
        symptoms: Set<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState =
        when {
            hasTemperature(symptoms) -> currentState.extend(symptoms, today)
            else -> DefaultState
        }

    fun expireExposedState(currentState: UserState): UserState =
        if (currentState is ExposedState && currentState.hasExpired())
            DefaultState
        else
            currentState

    fun isSymptomatic(symptoms: Set<Symptom>): Boolean =
        hasTemperature(symptoms) || hasCough(symptoms) || hasAnosmia(symptoms)

    fun transitionOnContactAlert(currentState: UserState): UserState? =
        when (currentState) {
            is DefaultState -> UserState.exposed()
            else -> null
        }

    fun transitionOnTestResult(
        currentState: UserState,
        testInfo: TestInfo
    ): UserState =
        when (testInfo.result) {
            TestResult.NEGATIVE -> handleNegativeTestResult(currentState, testInfo.date)
            TestResult.POSITIVE -> handlePositiveTestResult(currentState, testInfo.date)
            TestResult.INVALID -> currentState
        }

    private fun handleNegativeTestResult(state: UserState, testDate: DateTime): UserState =
        when (state) {
            is SymptomaticState ->
                if (state.since.isAfter(testDate)) state else DefaultState
            is PositiveState ->
                if (state.since.isAfter(testDate)) state else DefaultState
            is ExposedState ->
                if (state.since.isAfter(testDate)) state else DefaultState
            else ->
                DefaultState
        }

    private fun handlePositiveTestResult(state: UserState, testDate: DateTime): UserState =
        when (state) {
            is SymptomaticState ->
                positive(testDate, state.symptoms)
            is PositiveState ->
                state
            is ExposedState ->
                if (state.since.isAfter(testDate)) state else positive(testDate)
            is DefaultState ->
                positive(testDate)
        }

    private fun hasTemperature(symptoms: Set<Symptom>): Boolean =
        TEMPERATURE in symptoms

    private fun hasCough(symptoms: Set<Symptom>): Boolean =
        COUGH in symptoms

    private fun hasAnosmia(symptoms: Set<Symptom>): Boolean =
        ANOSMIA in symptoms
}
