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
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import uk.nhs.nhsx.sonar.android.app.util.toUtc
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized

class UserStateTransitionsOnNegativeResultTest {

    @Test
    fun `default remains default`() {
        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now().toUtc())

        val state = transitionOnTestResult(DefaultState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `symptomatic, if symptoms onset is prior test, stays symptomatic but expired`() {
        val symptomDate = LocalDate.now().minusDays(1)
        val symptomatic = UserState.symptomatic(symptomDate, nonEmptySetOf(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, symptomatic.since.plusDays(1))

        val state = transitionOnTestResult(symptomatic, testInfo)

        assertThat(state.hasExpired()).isTrue()
        assertThat(state).isEqualTo(symptomatic.copy(until = yesterday()))
    }

    @Test
    fun `symptomatic, if symptoms onset is after test, remains symptomatic`() {
        val symptomDate = LocalDate.now().minusDays(2)
        val symptomatic = UserState.symptomatic(symptomDate, nonEmptySetOf(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, symptomatic.since.minusDays(1))

        val state = transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(symptomatic)
    }

    @Test
    fun `exposed symptomatic, becomes exposed`() {
        val date = LocalDate.now().minusDays(6)
        val exposed = UserState.exposed(date)
        val exposedSymptomatic = ExposedSymptomaticState(
            exposed.since,
            exposed.until,
            nonEmptySetOf(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, exposed.since.plusDays(1))

        val state = transitionOnTestResult(exposedSymptomatic, testInfo)

        assertThat(state).isEqualTo(exposed)
    }

    @Test
    fun `positive, if new test is after current one, becomes default`() {
        val testDate = DateTime.now().minusDays(6)
        val positive = UserState.positive(testDate, nonEmptySetOf(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, positive.since.plusDays(1))

        val state = transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `positive, if new test is before current one, remains positive`() {
        val testDate = DateTime.now().minusDays(2)
        val positive = UserState.positive(testDate, nonEmptySetOf(COUGH))
        val testInfo = TestInfo(TestResult.NEGATIVE, positive.since.minusDays(1))

        val state = transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(positive)
    }

    @Test
    fun `exposed, remains exposed`() {
        val date = LocalDate.now().minusDays(6)
        val exposed = UserState.exposed(date)
        val testInfo = TestInfo(TestResult.NEGATIVE, exposed.since.plusDays(1))

        val state = transitionOnTestResult(exposed, testInfo)

        assertThat(state).isEqualTo(exposed)
    }

    private fun yesterday() = LocalDate.now().minusDays(1).toUtcNormalized()
}
