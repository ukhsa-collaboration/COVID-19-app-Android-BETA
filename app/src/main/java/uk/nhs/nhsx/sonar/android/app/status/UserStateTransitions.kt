/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.DateTime
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NUMBER_OF_DAYS_IN_SYMPTOMATIC
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.exposed
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.positive
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.isEarlierThan
import javax.inject.Inject

class UserStateTransitions @Inject constructor() {

    /**
     * User can do the questionnaire when in the
     * Default or Exposed states.
     */
    fun diagnose(
        currentState: UserState,
        symptomsDate: LocalDate,
        symptoms: NonEmptySet<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState {

        return when {
            !isolationNeeded(symptomsDate, symptoms, today) ->
                currentState
            currentState is ExposedState ->
                UserState.exposedSymptomatic(symptomsDate, currentState, symptoms)
            else ->
                UserState.symptomatic(symptomsDate, symptoms, today)
        }
    }

    /**
     * After 7 days of being symptomatic/positive
     * or after 14 days in exposed-symptomatic
     * The user needs to answer the check in questionnaire.
     */
    fun diagnoseForCheckin(
        currentState: UserState,
        symptoms: Set<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState =
        when {
            hasTemperature(symptoms) -> currentState.extendAfterCheckin(symptoms, today)
            else -> DefaultState
        }

    fun transitionOnExpiredExposedState(currentState: UserState): UserState =
        if (currentState is ExposedState && currentState.hasExpired())
            DefaultState
        else
            currentState

    fun transitionOnExposure(
        currentState: UserState,
        exposureDate: DateTime
    ): UserState =
        when (currentState) {
            is DefaultState -> exposed(exposureDate.toLocalDate())
            else -> currentState
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
            is ExposedSymptomaticState ->
                when {
                    state.since.isAfter(testDate) -> state
                    state.isWithinExposureWindow() -> exposed(state)
                    else -> DefaultState
                }
            is PositiveState -> state
            is ExposedState -> state
            is DefaultState -> DefaultState
        }

    private fun handlePositiveTestResult(state: UserState, testDate: DateTime): UserState =
        when (state) {
            is SymptomaticState -> positive(state)
            is ExposedSymptomaticState -> positive(state)
            is PositiveState -> state
            is ExposedState -> positive(testDate)
            is DefaultState -> positive(testDate)
        }

    private fun isolationNeeded(
        symptomsDate: LocalDate,
        symptoms: NonEmptySet<Symptom>,
        today: LocalDate
    ): Boolean {
        val isInSevenDayWindow = !symptomsDate.isEarlierThan(NUMBER_OF_DAYS_IN_SYMPTOMATIC, today)

        return isInSevenDayWindow || hasTemperature(symptoms)
    }

    private fun hasTemperature(symptoms: Set<Symptom>): Boolean =
        TEMPERATURE in symptoms
}
