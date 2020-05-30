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
import uk.nhs.nhsx.sonar.android.app.util.atSevenAm
import uk.nhs.nhsx.sonar.android.app.util.toUtc
import uk.nhs.nhsx.sonar.android.app.util.toUtcNormalized

class UserStateTransitionsOnPositiveResultTest {

    @Test
    fun `default becomes positive, with no symptoms`() {
        val testInfo = TestInfo(TestResult.POSITIVE, DateTime.now().toUtc())

        val state = transitionOnTestResult(DefaultState, testInfo)

        val since = testInfo.date.toLocalDate().atSevenAm().toUtc()
        val until = testInfo.date.toLocalDate().plusDays(7).atSevenAm().toUtc()

        assertThat(state).isEqualTo(PositiveState(since, until, emptySet()))
    }

    @Test
    fun `symptomatic becomes positive and the symptoms and duration are retained`() {
        val testDate = LocalDate.now().plusDays(2).toDateTime(LocalTime.parse("14:00:00"))
        val testInfo = TestInfo(TestResult.POSITIVE, testDate)

        val symptomatic = buildSymptomaticState()

        val state = transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(
            PositiveState(
                since = testInfo.date.toLocalDate().toUtcNormalized(),
                until = symptomatic.until,
                symptoms = symptomatic.symptoms
            )
        )
    }

    @Test
    fun `exposed-symptomatic becomes positive and the symptoms and duration are retained`() {
        val testDate = LocalDate.now().plusDays(2).toDateTime(LocalTime.parse("14:00:00"))
        val testInfo = TestInfo(TestResult.POSITIVE, testDate)

        val exposedSymptomatic = buildExposedSymptomaticState()

        val state = transitionOnTestResult(exposedSymptomatic, testInfo)

        assertThat(state).isEqualTo(
            PositiveState(
                since = testInfo.date.toLocalDate().toUtcNormalized(),
                until = exposedSymptomatic.until,
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
        val expectedUntil = expectedSince.plusDays(7)

        assertThat(state).isEqualTo(PositiveState(
            expectedSince,
            expectedUntil,
            emptySet()
        ))
    }
}
