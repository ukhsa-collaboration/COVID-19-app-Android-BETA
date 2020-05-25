/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.Symptom.ANOSMIA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.NAUSEA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.SNEEZE
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnTestResult
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.diagnose
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.diagnoseForCheckin
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.expireExposedState
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.isSymptomatic
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnContactAlert
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.atSevenAm
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtc

class UserStateTransitionsTest {

    private val today = LocalDate(2020, 4, 10)
    private val symptomsWithoutTemperature = nonEmptySetOf(COUGH)
    private val symptomsWithTemperature = nonEmptySetOf(TEMPERATURE)

    @Before
    fun setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().millis)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, and no temperature`() {
        val sevenDaysAgoOrMore = today.minusDays(7)

        val state = diagnose(DefaultState, sevenDaysAgoOrMore, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, no temperature, and current state is Exposed`() {
        val exposedState = buildExposedState()
        val sevenDaysAgoOrMore = today.minusDays(7)

        val state = diagnose(exposedState, sevenDaysAgoOrMore, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(exposedState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, with temperature`() {
        val sevenDaysAgoOrMore = today.minusDays(7)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(
            DefaultState,
            sevenDaysAgoOrMore,
            symptomsWithTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                sevenDaysAgoOrMore.atSevenAm().toUtc(),
                sevenDaysAfterSymptoms,
                symptomsWithTemperature)
        )
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, and no temperature`() {
        val lessThanSevenDaysAgo = today.minusDays(6)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(
            DefaultState,
            lessThanSevenDaysAgo,
            symptomsWithoutTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                lessThanSevenDaysAgo.atSevenAm().toUtc(),
                sevenDaysAfterSymptoms,
                symptomsWithoutTemperature
            )
        )
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, with temperature`() {
        val lessThanSevenDaysAgo = today.minusDays(6)
        val sevenDaysAfterSymptoms = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(
            DefaultState,
            lessThanSevenDaysAgo,
            symptomsWithTemperature,
            today
        )

        assertThat(state).isEqualTo(
            SymptomaticState(
                lessThanSevenDaysAgo.atSevenAm().toUtc(),
                sevenDaysAfterSymptoms,
                symptomsWithTemperature
            )
        )
    }

    @Test
    fun `diagnoseForCheckin - with temperature`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnoseForCheckin(tomorrow, setOf(TEMPERATURE), today)

        assertThat(state).isEqualTo(CheckinState(tomorrow, tomorrow, nonEmptySetOf(TEMPERATURE)))
    }

    @Test
    fun `diagnoseForCheckin - with cough and temperature`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnoseForCheckin(tomorrow, setOf(COUGH, TEMPERATURE), today)

        assertThat(state).isEqualTo(CheckinState(tomorrow, tomorrow, nonEmptySetOf(COUGH, TEMPERATURE)))
    }

    @Test
    fun `diagnoseForCheckin - with cough`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnoseForCheckin(tomorrow, setOf(COUGH), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnoseForCheckin - with anosmia`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnoseForCheckin(tomorrow, setOf(ANOSMIA), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `diagnoseForCheckin - with no symptoms`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnoseForCheckin(tomorrow, emptySet(), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `test transitionOnContactAlert`() {
        assertThat(transitionOnContactAlert(DefaultState)).isInstanceOf(ExposedState::class.java)
        assertThat(transitionOnContactAlert(buildExposedState())).isNull()
        assertThat(transitionOnContactAlert(buildSymptomaticState())).isNull()
        assertThat(transitionOnContactAlert(buildCheckinState())).isNull()
    }

    @Test
    fun `test expireExposedState`() {
        val exposedState = buildExposedState()
        val symptomaticState = buildSymptomaticState()
        val checkinState = buildCheckinState()

        val expiredExposedState = buildExposedState(until = DateTime.now().minusSeconds(1))
        val expiredSymptomaticState = buildSymptomaticState(until = DateTime.now().minusSeconds(1))
        val expiredCheckinState = buildCheckinState(until = DateTime.now().minusSeconds(1))

        assertThat(expireExposedState(DefaultState)).isEqualTo(DefaultState)
        assertThat(expireExposedState(exposedState)).isEqualTo(exposedState)
        assertThat(expireExposedState(symptomaticState)).isEqualTo(symptomaticState)
        assertThat(expireExposedState(checkinState)).isEqualTo(checkinState)
        assertThat(expireExposedState(expiredSymptomaticState)).isEqualTo(expiredSymptomaticState)
        assertThat(expireExposedState(expiredCheckinState)).isEqualTo(expiredCheckinState)

        assertThat(expireExposedState(expiredExposedState)).isEqualTo(DefaultState)
    }

    @Test
    fun `isSymptomatic - with cough, temperature or loss of smell`() {
        assertThat(isSymptomatic(setOf(COUGH))).isTrue()
        assertThat(isSymptomatic(setOf(TEMPERATURE))).isTrue()
        assertThat(isSymptomatic(setOf(ANOSMIA))).isTrue()
    }

    @Test
    fun `isSymptomatic - with anything other than cough, temperature or loss of smell`() {
        assertThat(isSymptomatic(setOf(NAUSEA, SNEEZE))).isFalse()
    }

    @Test
    fun `transitionOnTestResult - with negative result to default state`() {
        val currentState = DefaultState

        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now().toUtc())

        val state = transitionOnTestResult(currentState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `transitionOnTestResult - with negative result and test date prior to symptoms date to SymptomaticState`() {
        val symptomDate = LocalDate.now().minusDays(2)
        val currentState = UserState.symptomatic(symptomDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now().minusDays(6))

        val state = transitionOnTestResult(currentState, testInfo)

        assertThat(state).isEqualTo(currentState)
    }

    @Test
    fun `transitionOnTestResult - with negative result and test date after the date of symptoms to SymptomaticState`() {
        val symptomDate = LocalDate.now().minusDays(6)
        val currentState = UserState.symptomatic(symptomDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now().minusDays(3))

        val state = transitionOnTestResult(currentState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @After
    fun tearDown() {
        DateTimeUtils.setCurrentMillisSystem()
    }
}
