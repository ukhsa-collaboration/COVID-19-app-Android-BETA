/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnTestResult
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.atSevenAm
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtc

class UserStateTransitionsOnPositiveResultTest {

    @Test
    fun `default becomes positive`() {
        val testInfo = TestInfo(TestResult.POSITIVE, DateTime.now().toUtc())

        val state = transitionOnTestResult(DefaultState, testInfo)

        val since = testInfo.date.toLocalDate().atSevenAm().toUtc()
        val until = testInfo.date.toLocalDate().plusDays(7).atSevenAm().toUtc()

        assertThat(state).isEqualTo(PositiveState(since, until, nonEmptySetOf(TEMPERATURE)))
    }

    @Test
    fun `symptomatic becomes positive`() {
        val symptomDate = LocalDate.now().minusDays(6)
        val symptomatic = UserState.symptomatic(symptomDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.POSITIVE, symptomatic.since.plusDays(1))

        val state = transitionOnTestResult(symptomatic, testInfo)

        val since = testInfo.date.toLocalDate().atSevenAm().toUtc()
        val until = testInfo.date.toLocalDate().plusDays(7).atSevenAm().toUtc()

        assertThat(state).isEqualTo(PositiveState(since, until, symptomatic.symptoms))
    }

    @Test
    fun `positive remains positive`() {
        val testDate = DateTime.now().minusDays(6)
        val positive = UserState.positive(testDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.POSITIVE, positive.since.plusDays(1))

        val state = transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(positive)
    }

    @Test
    fun `checkin becomes positive`() {
        val checkinSince = LocalDate.now().minusDays(6).atSevenAm().toUtc()
        val checkin = UserState.checkin(checkinSince, NonEmptySet.create(TEMPERATURE))
        val testInfo = TestInfo(TestResult.POSITIVE, checkin.since.plusDays(1))

        val state = transitionOnTestResult(checkin, testInfo)

        val since = testInfo.date.toLocalDate().atSevenAm().toUtc()
        val until = testInfo.date.toLocalDate().plusDays(7).atSevenAm().toUtc()

        assertThat(state).isEqualTo(PositiveState(since, until, checkin.symptoms))
    }

    @Test
    fun `exposed, if exposed prior test, becomes positive`() {
        val date = LocalDate.now().minusDays(6)
        val currentState = UserState.exposed(date)
        val testInfo = TestInfo(TestResult.POSITIVE, currentState.since.plusDays(1))

        val state = transitionOnTestResult(currentState, testInfo)

        val since = testInfo.date.toLocalDate().atSevenAm().toUtc()
        val until = testInfo.date.toLocalDate().plusDays(7).atSevenAm().toUtc()

        assertThat(state).isEqualTo(PositiveState(since, until, nonEmptySetOf(TEMPERATURE)))
    }

    @Test
    fun `exposed, if exposed after test, remains exposed`() {
        val date = LocalDate.now().minusDays(2)
        val exposed = UserState.exposed(date)
        val testInfo = TestInfo(TestResult.POSITIVE, exposed.since.minusDays(1))

        val state = transitionOnTestResult(exposed, testInfo)

        assertThat(state).isEqualTo(exposed)
    }
}
