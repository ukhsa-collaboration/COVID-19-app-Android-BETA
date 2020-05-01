package uk.nhs.nhsx.sonar.android.app.status

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateTest {

    private fun buildRedState(
        until: DateTime = DateTime.now(),
        symptoms: NonEmptySet<Symptom> = nonEmptySetOf(COUGH)
    ) = RedState(until, symptoms)

    @Test
    fun `test RedState`() {
        val redState = buildRedState()

        assertThat(redState.isOk()).isFalse()
        assertThat(redState.isAtRisk()).isFalse()
        assertThat(redState.shouldIsolate()).isTrue()
        assertThat(redState.transitionOnContactAlert()).isNull()
        assertThat(redState.symptoms()).isEqualTo(redState.symptoms)
    }

    @Test
    fun `test RedState when not expired`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)
        val notExpired = buildRedState(until = DateTime.now().plusSeconds(2))

        assertThat(notExpired.hasExpired()).isFalse()
        assertThat(notExpired.transitionIfExpired()).isNull()

        notExpired.scheduleCheckInReminder(reminders)
        verify { reminders.scheduleCheckInReminder(notExpired.until) }
    }

    @Test
    fun `test RedState when expired`() {
        val reminders = mockk<Reminders>()
        val expired = buildRedState(until = DateTime.now().minusSeconds(1))

        assertThat(expired.hasExpired()).isTrue()
        assertThat(expired.transitionIfExpired()).isInstanceOf(DefaultState::class.java)

        expired.scheduleCheckInReminder(reminders)
        verify { reminders wasNot Called }
    }

    @Test
    fun `test DefaultState`() {
        val defaultState = DefaultState()

        assertThat(defaultState.isOk()).isTrue()
        assertThat(defaultState.isAtRisk()).isFalse()
        assertThat(defaultState.shouldIsolate()).isFalse()
        assertThat(defaultState.transitionOnContactAlert()).isInstanceOf(EmberState::class.java)
        assertThat(defaultState.symptoms()).isEmpty()
    }

    @Test
    fun `test DefaultState when not expired`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)
        val notExpired = DefaultState(until = DateTime.now().plusSeconds(2))

        assertThat(notExpired.hasExpired()).isFalse()
        assertThat(notExpired.transitionIfExpired()).isNull()

        notExpired.scheduleCheckInReminder(reminders)
        verify { reminders wasNot Called }
    }

    @Test
    fun `test DefaultState when expired`() {
        val reminders = mockk<Reminders>()
        val expired = DefaultState(until = DateTime.now().minusSeconds(1))

        assertThat(expired.hasExpired()).isTrue()
        assertThat(expired.transitionIfExpired()).isInstanceOf(DefaultState::class.java)

        expired.scheduleCheckInReminder(reminders)
        verify { reminders wasNot Called }
    }
}
