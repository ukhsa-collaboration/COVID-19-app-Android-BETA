/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult

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
    fun `exposed symptomatic, becomes exposed`() {
        val exposedSymptomatic = buildExposedSymptomaticState()
        val testInfo = TestInfo(TestResult.NEGATIVE, exposedSymptomatic.since.plusDays(1))

        val state = transitions.transitionOnTestResult(exposedSymptomatic, testInfo)

        assertThat(state).isEqualTo(ExposedState(
            since = exposedSymptomatic.since,
            until = exposedSymptomatic.until
        ))
    }

    @Test
    fun `positive remains positive`() {
        val positive = buildPositiveState()
        val testInfo = TestInfo(TestResult.NEGATIVE, positive.since.minusDays(1))

        val state = transitions.transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(positive)
    }

    @Test
    fun `exposed, remains exposed`() {
        val exposed = buildExposedState()
        val testInfo = TestInfo(TestResult.NEGATIVE, exposed.since.plusDays(1))

        val state = transitions.transitionOnTestResult(exposed, testInfo)

        assertThat(state).isEqualTo(exposed)
    }

}
