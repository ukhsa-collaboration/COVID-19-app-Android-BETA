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
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized

class UserStateTransitionsOnNegativeResultTest {
    private val transitions = UserStateTransitions()

    @Test
    fun `default remains default`() {
        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now())

        val state = transitions.transitionOnTestResult(DefaultState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `symptomatic, if symptoms onset is prior test, becomes default`() {
        val symptomatic = buildSymptomaticState()
        val testInfo = TestInfo(TestResult.NEGATIVE, symptomatic.since.plusDays(1))

        val state = transitions.transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `symptomatic, if symptoms onset is after test, remains symptomatic`() {
        val symptomatic = buildSymptomaticState()
        val testInfo = TestInfo(TestResult.NEGATIVE, symptomatic.since.minusDays(1))

        val state = transitions.transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(symptomatic)
    }

    @Test
    fun `exposed-symptomatic, if symptoms onset is prior to test (within exposure window), becomes exposed`() {
        val symptomDate = LocalDate.now().minusDays(10).toUtcNormalized()
        val exposedDate = symptomDate.minusDays(1)
        val testDateAfterSymptomatic = symptomDate.plusDays(1)
        val exposedSymptomatic = buildExposedSymptomaticState(
            since = symptomDate,
            until = LocalDate.now().toUtcNormalized(),
            exposedAt = exposedDate
        )
        val testInfo = TestInfo(TestResult.NEGATIVE, testDateAfterSymptomatic)

        val state = transitions.transitionOnTestResult(exposedSymptomatic, testInfo)

        assertThat(state).isEqualTo(
            ExposedState(
                since = exposedDate,
                until = exposedSymptomatic.until
            )
        )
    }

    @Test
    fun `exposed-symptomatic, if symptoms onset is prior to test (outside exposure window), becomes default`() {
        val symptomDate = LocalDate.now().minusDays(15).toUtcNormalized()
        val exposedDate = symptomDate.minusDays(5)
        val testDateAfterSymptomatic = symptomDate.plusDays(1)
        val exposedSymptomatic = buildExposedSymptomaticState(
            since = symptomDate,
            until = LocalDate.now().toUtcNormalized(),
            exposedAt = exposedDate
        )
        val testInfo = TestInfo(TestResult.NEGATIVE, testDateAfterSymptomatic)

        val state = transitions.transitionOnTestResult(exposedSymptomatic, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `exposed-symptomatic, if symptoms onset is after test, remains exposed-symptomatic`() {
        val symptomDate = LocalDate.now().minusDays(15).toUtcNormalized()
        val exposedDate = symptomDate.minusDays(5)
        val testDateBeforeSymptomatic = symptomDate.minusDays(1)
        val exposedSymptomatic = buildExposedSymptomaticState(
            since = symptomDate,
            until = LocalDate.now().toUtcNormalized(),
            exposedAt = exposedDate
        )
        val testInfo = TestInfo(TestResult.NEGATIVE, testDateBeforeSymptomatic)

        val state = transitions.transitionOnTestResult(exposedSymptomatic, testInfo)

        assertThat(state).isEqualTo(exposedSymptomatic)
    }

    @Test
    fun `positive remains positive`() {
        val positive = buildPositiveState()
        val testInfo = TestInfo(TestResult.NEGATIVE, positive.since.minusDays(1))

        val state = transitions.transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(positive)
    }

    @Test
    fun `exposed remains exposed`() {
        val exposed = buildExposedState()
        val testInfo = TestInfo(TestResult.NEGATIVE, exposed.since.plusDays(1))

        val state = transitions.transitionOnTestResult(exposed, testInfo)

        assertThat(state).isEqualTo(exposed)
    }
}
