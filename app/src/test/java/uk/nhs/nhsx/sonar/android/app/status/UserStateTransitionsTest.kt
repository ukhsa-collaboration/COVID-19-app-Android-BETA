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
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionIfExpired
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions.transitionOnContactAlert
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateTransitionsTest {

    private val today = LocalDate(2020, 4, 10)

    @Test
    fun `diagnose - when symptoms date is today, red state is valid until 7 days after today`() {
        val state = diagnose(today, nonEmptySetOf(COUGH), today)

        assertThat(state).isInstanceOf(RedState::class.java)
        assertThat(state.until()).isEqualTo(DateTime(2020, 4, 17, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `diagnose - when symptoms date is yesterday, red state is valid until 6 days after today`() {
        val state = diagnose(today.minusDays(1), nonEmptySetOf(COUGH), today)

        assertThat(state).isInstanceOf(RedState::class.java)
        assertThat(state.until()).isEqualTo(DateTime(2020, 4, 16, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `diagnose - when symptoms date is 8 days ago without temperature, state should be recovery`() {
        val state = diagnose(today.minusDays(8), nonEmptySetOf(COUGH), today)

        assertThat(state).isInstanceOf(RecoveryState::class.java)
    }

    @Test
    fun `diagnose - when symptoms date is 8 days ago with temperature, state should be red until next day`() {
        val state = diagnose(today.minusDays(8), nonEmptySetOf(TEMPERATURE), today)

        assertThat(state).isInstanceOf(RedState::class.java)
        assertThat(state.until()).isEqualTo(DateTime(2020, 4, 11, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago without temperature, state should be recovery`() {
        val state = diagnose(today.minusDays(7), nonEmptySetOf(COUGH), today)

        assertThat(state).isInstanceOf(RecoveryState::class.java)
    }

    @Test
    fun `diagnose - when symptoms date is 7 days ago with temperature, state should be red until next day`() {
        val state = diagnose(today.minusDays(7), nonEmptySetOf(TEMPERATURE), today)

        assertThat(state).isInstanceOf(RedState::class.java)
        assertThat(state.until()).isEqualTo(DateTime(2020, 4, 11, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `diagnose - when symptoms date is 6 days ago with temperature, state should be red`() {
        val state = diagnose(today.minusDays(6), nonEmptySetOf(TEMPERATURE), today)

        assertThat(state).isInstanceOf(RedState::class.java)
    }

    @Test
    fun `diagnoseForChecking - with temperature - state should be checkin until tomorrow`() {
        val state = diagnoseForCheckin(nonEmptySetOf(TEMPERATURE), today = today)

        assertThat(state).isInstanceOf(CheckinState::class.java)
        assertThat(state.until()).isEqualTo(DateTime(2020, 4, 11, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `diagnoseForChecking - with cough and temperature - state should be checkin until tomorrow`() {
        val state = diagnoseForCheckin(setOf(COUGH, TEMPERATURE), today = today)

        assertThat(state).isInstanceOf(CheckinState::class.java)
        assertThat(state.until()).isEqualTo(DateTime(2020, 4, 11, 7, 0).toDateTime(UTC))
    }

    @Test
    fun `diagnoseForChecking - with cough - state should be recovery`() {
        val state = diagnoseForCheckin(setOf(COUGH), today = today)

        assertThat(state).isInstanceOf(RecoveryState::class.java)
    }

    @Test
    fun `diagnoseForChecking - with no symptoms - state should be default`() {
        val state = diagnoseForCheckin(emptySet(), today = today)

        assertThat(state).isInstanceOf(DefaultState::class.java)
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
    fun `test transitionIfExpired`() {
        assertThat(transitionIfExpired(DefaultState)).isNull()
        assertThat(transitionIfExpired(RecoveryState)).isNull()
        assertThat(transitionIfExpired(amberState)).isNull()
        assertThat(transitionIfExpired(redState)).isNull()
        assertThat(transitionIfExpired(checkinState)).isNull()

        assertThat(transitionIfExpired(expiredAmberState)).isEqualTo(DefaultState)
        assertThat(transitionIfExpired(expiredRedState)).isEqualTo(DefaultState)
        assertThat(transitionIfExpired(expiredCheckinState)).isEqualTo(DefaultState)
    }
}
