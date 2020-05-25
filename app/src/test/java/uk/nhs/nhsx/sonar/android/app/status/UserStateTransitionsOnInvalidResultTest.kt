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
import uk.nhs.nhsx.sonar.android.app.util.toUtc

class UserStateTransitionsOnInvalidResultTest {

    @Test
    fun `default remains default`() {
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now().toUtc())

        val state = transitionOnTestResult(DefaultState, testInfo)

        assertThat(state).isEqualTo(DefaultState)
    }

    @Test
    fun `symptomatic remains symptomatic`() {
        val symptomatic = UserState.symptomatic(LocalDate.now(), NonEmptySet.create(COUGH))
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now().toUtc())

        val state = transitionOnTestResult(symptomatic, testInfo)

        assertThat(state).isEqualTo(symptomatic)
    }

    @Test
    fun `checkin remains checkin`() {
        val checkin = UserState.checkin(DateTime.now(), NonEmptySet.create(TEMPERATURE))
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now().toUtc())

        val state = transitionOnTestResult(checkin, testInfo)

        assertThat(state).isEqualTo(checkin)
    }

    @Test
    fun `exposed remains exposed`() {
        val exposed = UserState.exposed(LocalDate.now())
        val testInfo = TestInfo(TestResult.INVALID, DateTime.now().toUtc())

        val state = transitionOnTestResult(exposed, testInfo)

        assertThat(state).isEqualTo(exposed)
    }
}
