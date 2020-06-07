/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.Symptom.ANOSMIA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateStorageTest {

    private val transitions = mockk<UserStateTransitions>(relaxUnitFun = true)
    private val userStatePrefs = mockk<UserStatePrefs>(relaxUnitFun = true)
    private val reminders = mockk<Reminders>(relaxUnitFun = true)
    private val userInbox = mockk<UserInbox>(relaxUnitFun = true)

    private val userStateStorage = UserStateStorage(
        transitions = transitions,
        userStatePrefs = userStatePrefs,
        userInbox = userInbox,
        reminders = reminders
    )

    @Test
    fun `diagnose - delegates to transitions with current state, symptoms and their onset`() {
        val symptomsDate = LocalDate.now()
        val symptoms = nonEmptySetOf(COUGH, ANOSMIA, TEMPERATURE)
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns DefaultState

        userStateStorage.diagnose(symptomsDate, symptoms)

        verify {
            transitions.diagnose(DefaultState, symptomsDate, symptoms)
        }
    }

    @Test
    fun `diagnose - updates the state storage with new state`() {
        val state = buildSymptomaticState()
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns state

        userStateStorage.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            userStatePrefs.set(state)
        }
    }

    @Test
    fun `diagnose - cancels current reminder and schedules a new one for the new state`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns buildSymptomaticState()

        userStateStorage.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            reminders.cancelCheckinReminder()
            reminders.scheduleCheckInReminder(any())
        }
    }

    @Test
    fun `diagnose - adds recovery message to inbox when new state is default`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns DefaultState

        userStateStorage.diagnose(LocalDate.now().minusDays(10), nonEmptySetOf(COUGH))

        verify {
            userInbox.addRecovery()
        }
    }

    @Test
    fun `diagnose - does not add recovery message when state is not default`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns buildSymptomaticState()

        userStateStorage.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `diagnoseCheckIn - delegates to transitions with current state and symptoms`() {
        val symptoms = nonEmptySetOf(COUGH, TEMPERATURE)
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns DefaultState

        userStateStorage.diagnoseCheckIn(symptoms)

        verify {
            transitions.diagnoseForCheckin(DefaultState, symptoms)
        }
    }

    @Test
    fun `diagnoseCheckIn - updates the state storage with new state`() {
        every { userStatePrefs.get() } returns DefaultState
        val state = buildSymptomaticState()
        every { transitions.diagnoseForCheckin(any(), any()) } returns state

        userStateStorage.diagnoseCheckIn(setOf(TEMPERATURE))

        verify {
            userStatePrefs.set(state)
        }
    }

    @Test
    fun `diagnoseCheckIn - adds recovery message to inbox when new state is default`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns DefaultState

        userStateStorage.diagnoseCheckIn(setOf(COUGH))

        verify {
            userInbox.addRecovery()
        }
    }

    @Test
    fun `diagnoseCheckIn - does not add recovery message when state is default but user has no symptom`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns DefaultState

        userStateStorage.diagnoseCheckIn(emptySet())

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `diagnoseCheckIn - does not add recovery message when state is not default`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns buildSymptomaticState()

        userStateStorage.diagnoseCheckIn(setOf(TEMPERATURE))

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `transitionOnExpiredExposedState - delegates to transitions`() {
        val state = buildExposedState()
        every { userStatePrefs.get() } returns state
        every { transitions.transitionOnExpiredExposedState(any()) } returns DefaultState

        userStateStorage.transitionOnExpiredExposedState()

        verify {
            transitions.transitionOnExpiredExposedState(state)
        }
    }

    @Test
    fun `transitionOnExpiredExposedState - updates the state storage with new state`() {
        val state = buildExposedState()
        every { userStatePrefs.get() } returns state
        every { transitions.transitionOnExpiredExposedState(any()) } returns state

        userStateStorage.transitionOnExpiredExposedState()

        verify {
            userStatePrefs.set(state)
        }
    }

    @Test
    fun `transitionOnContactAlert - delegates to transition with current state and exposure date`() {
        val exposureDate = DateTime.now()
        val state = buildSymptomaticState()
        every { userStatePrefs.get() } returns state
        every { transitions.transitionOnContactAlert(any(), any()) } returns DefaultState

        userStateStorage.transitionOnContactAlert(exposureDate) {}

        verify {
            transitions.transitionOnContactAlert(state, exposureDate)
        }
    }

    @Test
    fun `transitionOnContactAlert - updates the state storage with new state`() {
        every { userStatePrefs.get() } returns DefaultState
        val state = buildExposedState()
        every { transitions.transitionOnContactAlert(any(), any()) } returns state

        userStateStorage.transitionOnContactAlert(DateTime.now()) {}

        verify {
            userStatePrefs.set(state)
        }
    }

    @Test
    fun `transitionOnContactAlert - executes callback the when state is changed`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.transitionOnContactAlert(any(), any()) } returns buildExposedState()

        val onStateChanged = mockk<() -> Unit>(relaxed = true)

        userStateStorage.transitionOnContactAlert(DateTime.now(), onStateChanged)

        verify {
            onStateChanged.invoke()
        }
    }

    @Test
    fun `transitionOnContactAlert - does not executes the callback when state is not changed`() {
        val state = buildSymptomaticState()
        every { userStatePrefs.get() } returns state
        every { transitions.transitionOnContactAlert(any(), any()) } returns state

        val onStateChanged = mockk<() -> Unit>()

        userStateStorage.transitionOnContactAlert(DateTime.now(), onStateChanged)

        verify {
            onStateChanged wasNot Called
        }
    }

    @Test
    fun `transitionOnTestResult - delegates to transitions with current state and test info`() {
        val state = buildPositiveState()
        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now())
        every { userStatePrefs.get() } returns state
        every { transitions.transitionOnTestResult(any(), any()) } returns DefaultState

        userStateStorage.transitionOnTestResult(testInfo)

        verify {
            transitions.transitionOnTestResult(state, testInfo)
        }
    }

    @Test
    fun `transitionOnTestResult - updates the state storage with new state`() {
        every { userStatePrefs.get() } returns DefaultState
        val state = buildSymptomaticState()
        every { transitions.transitionOnTestResult(any(), any()) } returns state

        userStateStorage.transitionOnTestResult(TestInfo(TestResult.POSITIVE, DateTime.now()))

        verify {
            userStatePrefs.set(state)
        }
    }

    @Test
    fun `transitionOnTestResult - cancels current reminder and schedules a new one for the new state`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.transitionOnTestResult(any(), any()) } returns buildSymptomaticState()

        userStateStorage.transitionOnTestResult(TestInfo(TestResult.POSITIVE, DateTime.now()))

        verify {
            reminders.cancelCheckinReminder()
            reminders.scheduleCheckInReminder(any())
        }
    }

    @Test
    fun `transitionOnTestResult - adds test info message in inbox`() {
        every { userStatePrefs.get() } returns DefaultState
        every { transitions.transitionOnTestResult(any(), any()) } returns DefaultState

        val testInfo = TestInfo(TestResult.POSITIVE, DateTime.now())
        userStateStorage.transitionOnTestResult(testInfo)

        verify {
            userInbox.addTestInfo(testInfo)
        }
    }
}
