package uk.nhs.nhsx.sonar.android.app.status

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateStorageTest {

    private val userStatePrefs = mockk<UserStatePrefs>(relaxed = true)

    private val userStateStorage = UserStateStorage(
        userStatePrefs = userStatePrefs,
        userInbox = mockk(),
        reminders = mockk()
    )

    private val date = DateTime("2020-04-23T18:34:00Z")

    private val onStateChanged = mockk<() -> Unit>(relaxed = true)

    @Test
    fun `executes callback when state transition happens for DefaultState`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.transitionOnContactAlert(
            date = date,
            onStateChanged = onStateChanged
        )

        val slot = slot<ExposedState>()
        verifyAll {
            userStatePrefs.get()
            userStatePrefs.set(capture(slot))
            onStateChanged.invoke()
        }

        assertThat(slot.captured).isEqualTo(UserState.exposed(date.toLocalDate()))
    }

    @Test
    fun `does not execute state transitions - in exposed state`() {
        val userState = UserState.exposed(LocalDate.now())

        assertNoStateTransitionsHappened(userState)
    }

    @Test
    fun `does not execute state transitions - in symptomatic state`() {
        val userState = UserState.symptomatic(
            symptomsDate = LocalDate.now(),
            symptoms = nonEmptySetOf(Symptom.COUGH)
        )

        assertNoStateTransitionsHappened(userState)
    }

    @Test
    fun `does not execute state transitions - in exposed symptomatic state`() {
        val userState = UserState.exposedSymptomatic(
            symptomsDate = LocalDate.now(),
            state = UserState.exposed(LocalDate.now()),
            symptoms = nonEmptySetOf(Symptom.COUGH)
        )

        assertNoStateTransitionsHappened(userState)
    }

    private fun assertNoStateTransitionsHappened(userState: UserState) {
        every { userStatePrefs.get() } returns userState

        userStateStorage.transitionOnContactAlert(
            date = date,
            onStateChanged = onStateChanged
        )

        verify(exactly = 1) {
            userStateStorage.get()
        }

        verify(exactly = 0) {
            userStateStorage.set(any())
            onStateChanged.invoke()
        }
    }
}
