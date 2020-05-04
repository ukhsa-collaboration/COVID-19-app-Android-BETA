package uk.nhs.nhsx.sonar.android.app.status

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

private fun buildEmberState(
    until: DateTime = DateTime.now().plusDays(1)
) = EmberState(until)

private fun buildRedState(
    until: DateTime = DateTime.now().plusDays(1),
    symptoms: NonEmptySet<Symptom> = nonEmptySetOf(COUGH)
) = RedState(until, symptoms)

private fun buildCheckinState(
    until: DateTime = DateTime.now().plusDays(1),
    symptoms: NonEmptySet<Symptom> = nonEmptySetOf(COUGH)
) = CheckinState(until, symptoms)

class UserStateTest {

    private val emberState = buildEmberState()
    private val redState = buildRedState()
    private val checkinState = buildCheckinState()

    private val expiredEmberState = buildEmberState(until = DateTime.now().minusSeconds(1))
    private val expiredRedState = buildRedState(until = DateTime.now().minusSeconds(1))
    private val expiredCheckinState = buildCheckinState(until = DateTime.now().minusSeconds(1))

    @Test
    fun `test until`() {
        assertThat(DefaultState.until()).isNull()
        assertThat(RecoveryState.until()).isNull()
        assertThat(emberState.until()).isEqualTo(emberState.until)
        assertThat(redState.until()).isEqualTo(redState.until)
        assertThat(checkinState.until()).isEqualTo(checkinState.until)
    }

    @Test
    fun `test hasExpired`() {
        assertThat(DefaultState.hasExpired()).isFalse()
        assertThat(RecoveryState.hasExpired()).isFalse()
        assertThat(emberState.hasExpired()).isFalse()
        assertThat(redState.hasExpired()).isFalse()
        assertThat(checkinState.hasExpired()).isFalse()

        assertThat(expiredEmberState.hasExpired()).isTrue()
        assertThat(expiredRedState.hasExpired()).isTrue()
        assertThat(expiredCheckinState.hasExpired()).isTrue()
    }

    @Test
    fun `test displayState`() {
        assertThat(DefaultState.displayState()).isEqualTo(DisplayState.OK)
        assertThat(RecoveryState.displayState()).isEqualTo(DisplayState.OK)
        assertThat(emberState.displayState()).isEqualTo(DisplayState.AT_RISK)
        assertThat(redState.displayState()).isEqualTo(DisplayState.ISOLATE)
        assertThat(checkinState.displayState()).isEqualTo(DisplayState.ISOLATE)
    }

    @Test
    fun `test transitionOnContactAlert`() {
        assertThat(DefaultState.transitionOnContactAlert()).isInstanceOf(EmberState::class.java)
        assertThat(RecoveryState.transitionOnContactAlert()).isInstanceOf(EmberState::class.java)
        assertThat(emberState.transitionOnContactAlert()).isNull()
        assertThat(redState.transitionOnContactAlert()).isNull()
        assertThat(checkinState.transitionOnContactAlert()).isNull()
    }

    @Test
    fun `test transitionIfExpired`() {
        assertThat(DefaultState.transitionIfExpired()).isNull()
        assertThat(RecoveryState.transitionIfExpired()).isNull()
        assertThat(emberState.transitionIfExpired()).isNull()
        assertThat(redState.transitionIfExpired()).isNull()
        assertThat(checkinState.transitionIfExpired()).isNull()

        assertThat(expiredEmberState.transitionIfExpired()).isEqualTo(DefaultState)
        assertThat(expiredRedState.transitionIfExpired()).isEqualTo(DefaultState)
        assertThat(expiredCheckinState.transitionIfExpired()).isEqualTo(DefaultState)
    }

    @Test
    fun `test scheduleCheckInReminder`() {
        val reminders = mockk<Reminders>(relaxUnitFun = true)

        DefaultState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        RecoveryState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        emberState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        redState.scheduleCheckInReminder(reminders)
        verify(exactly = 1) { reminders.scheduleCheckInReminder(redState.until) }
        clearMocks(reminders)

        checkinState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredEmberState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredRedState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }

        expiredCheckinState.scheduleCheckInReminder(reminders)
        verify(exactly = 0) { reminders.scheduleCheckInReminder(any()) }
    }

    @Test
    fun `test symptoms`() {
        assertThat(DefaultState.symptoms()).isEmpty()
        assertThat(RecoveryState.symptoms()).isEmpty()
        assertThat(emberState.symptoms()).isEmpty()
        assertThat(redState.symptoms()).isEqualTo(redState.symptoms)
        assertThat(checkinState.symptoms()).isEqualTo(checkinState.symptoms)
    }
}
