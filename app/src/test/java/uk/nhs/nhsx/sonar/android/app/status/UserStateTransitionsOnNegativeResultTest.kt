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
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnTestResult
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.toUtc

class UserStateTransitionsOnNegativeResultTest {

    @Test
    fun `default remains default`() {
        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now().toUtc())

        val state = transitionOnTestResult(DefaultState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `symptomatic, if symptoms onset is prior test, becomes default`() {
        val symptomDate = LocalDate.now().minusDays(6)
        val symptomatic = UserState.symptomatic(symptomDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, symptomatic.since.plusDays(1))

        val state = transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `symptomatic, if symptoms onset is after test, remains symptomatic`() {
        val symptomDate = LocalDate.now().minusDays(2)
        val symptomatic = UserState.symptomatic(symptomDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, symptomatic.since.minusDays(1))

        val state = transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(symptomatic)
    }

    @Test
    fun `positive, if new test is after current one, becomes default`() {
        val testDate = DateTime.now().minusDays(6)
        val positive = UserState.positive(testDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, positive.since.plusDays(1))

        val state = transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `positive, if new test is before current one, remains positive`() {
        val testDate = DateTime.now().minusDays(2)
        val positive = UserState.positive(testDate, NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, positive.since.minusDays(1))

        val state = transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(positive)
    }

    @Test
    fun `exposed, if exposed prior test, becomes default`() {
        val date = LocalDate.now().minusDays(6)
        val currentState = UserState.exposed(date)
        val testInfo = TestInfo(TestResult.NEGATIVE, currentState.since.plusDays(1))

        val state = transitionOnTestResult(currentState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `exposed, if exposed after test, remains symptomatic`() {
        val date = LocalDate.now().minusDays(2)
        val exposed = UserState.exposed(date)
        val testInfo = TestInfo(TestResult.NEGATIVE, exposed.since.minusDays(1))

        val state = transitionOnTestResult(exposed, testInfo)

        assertThat(state).isEqualTo(exposed)
    }
}
