/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.diagnose
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.diagnoseForCheckin
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.expireAmberState
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnContactAlert
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateTransitionsTest {

    private val today = LocalDate(2020, 4, 10)
    private val symptomsWithoutTemperature = nonEmptySetOf(COUGH)
    private val symptomsWithTemperature = nonEmptySetOf(TEMPERATURE)

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, and no temperature`() {
        val `7 days ago or more` = today.minusDays(7)

        val state = diagnose(DefaultState, `7 days ago or more`, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(RecoveryState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, no temperature, and current state is Amber`() {
        val amberState = buildAmberState()
        val `7 days ago or more` = today.minusDays(7)

        val state = diagnose(amberState, `7 days ago or more`, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(amberState)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago or more, with temperature`() {
        val `7 days ago or more` = today.minusDays(7)
        val `7 days after symptoms` = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(DefaultState, `7 days ago or more`, symptomsWithTemperature, today)

        assertThat(state).isEqualTo(RedState(`7 days after symptoms`, symptomsWithTemperature))
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, and no temperature`() {
        val `less than 7 days ago` = today.minusDays(6)
        val `7 days after symptoms` = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(DefaultState, `less than 7 days ago`, symptomsWithoutTemperature, today)

        assertThat(state).isEqualTo(RedState(`7 days after symptoms`, symptomsWithoutTemperature))
    }

    @Test
    fun `diagnose - when symptoms date is less than 7 days ago, with temperature`() {
        val `less than 7 days ago` = today.minusDays(6)
        val `7 days after symptoms` = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnose(DefaultState, `less than 7 days ago`, symptomsWithTemperature, today)

        assertThat(state).isEqualTo(RedState(`7 days after symptoms`, symptomsWithTemperature))
    }

    @Test
    fun `diagnoseForCheckin - with temperature`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnoseForCheckin(setOf(TEMPERATURE), today)

        assertThat(state).isEqualTo(CheckinState(tomorrow, nonEmptySetOf(TEMPERATURE)))
    }

    @Test
    fun `diagnoseForCheckin - with cough and temperature`() {
        val tomorrow = DateTime(2020, 4, 11, 7, 0).toDateTime(UTC)

        val state = diagnoseForCheckin(setOf(COUGH, TEMPERATURE), today)

        assertThat(state).isEqualTo(CheckinState(tomorrow, nonEmptySetOf(COUGH, TEMPERATURE)))
    }

    @Test
    fun `diagnoseForCheckin - with cough`() {
        val state = diagnoseForCheckin(setOf(COUGH), today)

        assertThat(state).isEqualTo(RecoveryState)
    }

    @Test
    fun `diagnoseForCheckin - with no symptoms`() {
        val state = diagnoseForCheckin(emptySet(), today)

        assertThat(state).isEqualTo(DefaultState)
    }

    private val amberState = buildAmberState()
    private val redState = buildRedState()
    private val checkinState = buildCheckinState()

    private val expiredAmberState = buildAmberState(until = DateTime.now().minusSeconds(1))
    private val expiredRedState = buildRedState(until = DateTime.now().minusSeconds(1))
    private val expiredCheckinState = buildCheckinState(until = DateTime.now().minusSeconds(1))

    @Test
    fun `test transitionOnContactAlert`() {
        assertThat(transitionOnContactAlert(DefaultState)).isInstanceOf(AmberState::class.java)
        assertThat(transitionOnContactAlert(RecoveryState)).isInstanceOf(AmberState::class.java)
        assertThat(transitionOnContactAlert(amberState)).isNull()
        assertThat(transitionOnContactAlert(redState)).isNull()
        assertThat(transitionOnContactAlert(checkinState)).isNull()
    }

    @Test
    fun `test expireAmberState`() {
        assertThat(expireAmberState(DefaultState)).isEqualTo(DefaultState)
        assertThat(expireAmberState(RecoveryState)).isEqualTo(RecoveryState)
        assertThat(expireAmberState(amberState)).isEqualTo(amberState)
        assertThat(expireAmberState(redState)).isEqualTo(redState)
        assertThat(expireAmberState(checkinState)).isEqualTo(checkinState)
        assertThat(expireAmberState(expiredRedState)).isEqualTo(expiredRedState)
        assertThat(expireAmberState(expiredCheckinState)).isEqualTo(expiredCheckinState)

        assertThat(expireAmberState(expiredAmberState)).isEqualTo(DefaultState)
    }
}
