/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnTestResult
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized

class UserStateTransitionsOnPositiveResultTest {

    @Test
    fun `default becomes positive, with no symptoms`() {
        val testInfo = TestInfo(TestResult.POSITIVE, DateTime.now())

        val state = transitionOnTestResult(DefaultState, testInfo)

        val since = testInfo.date.toLocalDate().toUtcNormalized()
        val until = since.plusDays(UserState.NUMBER_OF_DAYS_IN_SYMPTOMATIC)

        assertThat(state).isEqualTo(PositiveState(since, until, emptySet()))
    }

    @Test
    fun `symptomatic becomes positive and the symptoms and duration are retained`() {
        val testDate = LocalDate.now().plusDays(2).toDateTime(LocalTime.parse("14:00:00"))
        val testInfo = TestInfo(TestResult.POSITIVE, testDate)

        val symptomatic = buildSymptomaticState().let {
            it.copy(until = it.since.plusDays(11))
        }

        val state = transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(
            PositiveState(
                since = symptomatic.since,
                until = symptomatic.since.plusDays(UserState.NUMBER_OF_DAYS_IN_SYMPTOMATIC),
                symptoms = symptomatic.symptoms
            )
        )
    }

    @Test
    fun `exposed-symptomatic becomes positive and the symptoms and duration are retained`() {
        val testDate = LocalDate.now().plusDays(2).toDateTime(LocalTime.parse("14:00:00"))
        val testInfo = TestInfo(TestResult.POSITIVE, testDate)

        val exposedSymptomatic = buildExposedSymptomaticState().let {
            it.copy(until = it.since.plusDays(11))
        }

        val state = transitionOnTestResult(exposedSymptomatic, testInfo)

        assertThat(state).isEqualTo(
            PositiveState(
                since = exposedSymptomatic.since,
                until = exposedSymptomatic.since.plusDays(UserState.NUMBER_OF_DAYS_IN_SYMPTOMATIC),
                symptoms = exposedSymptomatic.symptoms
            )
        )
    }

    @Test
    fun `positive remains positive`() {
        val positive = buildPositiveState()
        val testInfo = TestInfo(TestResult.POSITIVE, DateTime.now())

        val state = transitionOnTestResult(positive, testInfo)

        assertThat(state).isEqualTo(positive)
    }

    @Test
    fun `exposed becomes positive with no symptoms`() {
        val exposed = buildExposedState()
        val testInfo = TestInfo(TestResult.POSITIVE, DateTime.now())

        val state = transitionOnTestResult(exposed, testInfo)

        val expectedSince = testInfo.date.toLocalDate().toUtcNormalized()
        val expectedUntil = expectedSince.plusDays(UserState.NUMBER_OF_DAYS_IN_SYMPTOMATIC)

        assertThat(state).isEqualTo(PositiveState(
            expectedSince,
            expectedUntil,
            emptySet()
        ))
    }
}
