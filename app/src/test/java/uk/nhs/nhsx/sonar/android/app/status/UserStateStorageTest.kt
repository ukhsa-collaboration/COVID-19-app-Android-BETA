package uk.nhs.nhsx.sonar.android.app.status

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateStorageTest {

    private val userStatePrefs = mockk<UserStatePrefs>(relaxUnitFun = true)
    private val reminders = mockk<Reminders>(relaxUnitFun = true)
    private val userInbox = mockk<UserInbox>(relaxUnitFun = true)

    private val userStateStorage = UserStateStorage(
        userStatePrefs = userStatePrefs,
        userInbox = userInbox,
        reminders = reminders
    )

    @Test
    fun `diagnose - updates the state storage with new state`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            userStatePrefs.set(match { it is SymptomaticState })
        }
    }

    @Test
    fun `diagnose - cancels current reminder and schedules a new one for the new state`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            reminders.cancelCheckinReminder()
            reminders.scheduleCheckInReminder(any())
        }
    }

    @Test
    fun `diagnose - adds recovery message to inbox when new state is default`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.diagnose(LocalDate.now().minusDays(10), nonEmptySetOf(COUGH))

        verify {
            userInbox.addRecovery()
        }
    }

    @Test
    fun `diagnose - does not add recovery message when state is not default`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `diagnoseCheckIn - updates the state storage with new state`() {
        every { userStatePrefs.get() } returns buildSymptomaticState()

        userStateStorage.diagnoseCheckIn(setOf(TEMPERATURE))

        verify {
            userStatePrefs.set(match { it is SymptomaticState })
        }
    }

    @Test
    fun `diagnoseCheckIn - adds recovery message to inbox when new state is default`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.diagnoseCheckIn(setOf(COUGH))

        verify {
            userInbox.addRecovery()
        }
    }

    @Test
    fun `diagnoseCheckIn - does not add recovery message when state is default but user has no symptom`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.diagnoseCheckIn(emptySet())

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `diagnoseCheckIn - does not add recovery message when state is not default`() {
        every { userStatePrefs.get() } returns buildExposedState()

        userStateStorage.diagnoseCheckIn(setOf(TEMPERATURE))

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `transitionOnExpiredExposedState - updates the state storage with new state`() {
        every { userStatePrefs.get() } returns buildExposedState(
            until = DateTime.now().minusDays(1)
        )

        userStateStorage.transitionOnExpiredExposedState()

        verify {
            userStatePrefs.set(match { it is DefaultState })
        }
    }

    @Test
    fun `transitionOnContactAlert - updates the state storage with new state`() {
        every { userStatePrefs.get() } returns DefaultState

        userStateStorage.transitionOnContactAlert(DateTime.now()) {}

        verify {
            userStatePrefs.set(match { it is ExposedState })
        }
    }

    @Test
    fun `transitionOnContactAlert - executes callback the when state is changed`() {
        every { userStatePrefs.get() } returns DefaultState

        val onStateChanged = mockk<() -> Unit>(relaxed = true)

        userStateStorage.transitionOnContactAlert(DateTime.now(), onStateChanged)

        verify {
            onStateChanged.invoke()
        }
    }

    @Test
    fun `transitionOnContactAlert - does not executes the callback when state is not changed`() {
        every { userStatePrefs.get() } returns buildSymptomaticState()

        val onStateChanged = mockk<() -> Unit>()

        userStateStorage.transitionOnContactAlert(DateTime.now(), onStateChanged)

        verify {
            onStateChanged wasNot Called
        }
    }
}
