/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderScheduler
import uk.nhs.nhsx.sonar.android.app.status.Symptom.ANOSMIA
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class UserStateMachineTest {

    private val transitions = mockk<UserStateTransitions>(relaxUnitFun = true)
    private val userStateStorage = mockk<UserStateStorage>(relaxUnitFun = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxUnitFun = true)
    private val userInbox = mockk<UserInbox>(relaxUnitFun = true)

    private val userStateMachine = UserStateMachine(
        transitions = transitions,
        userStateStorage = userStateStorage,
        userInbox = userInbox,
        reminderScheduler = reminderScheduler
    )

    @Test
    fun `diagnose - delegates to transitions with current state, symptoms and their onset`() {
        val symptomsDate = LocalDate.now()
        val symptoms = nonEmptySetOf(COUGH, ANOSMIA, TEMPERATURE)
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns DefaultState

        userStateMachine.diagnose(symptomsDate, symptoms)

        verify {
            transitions.diagnose(DefaultState, symptomsDate, symptoms)
        }
    }

    @Test
    fun `diagnose - when state is changed, saves the new state`() {
        val state = buildSymptomaticState()
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns state

        userStateMachine.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            userStateStorage.set(state)
        }
    }

    @Test
    fun `diagnose - when state is not changed, does not update the state storage`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns DefaultState

        userStateMachine.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify(exactly = 0) {
            userStateStorage.set(any())
        }
    }

    @Test
    fun `diagnose - when state is changed, cancels current reminder and schedules a new one`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns buildSymptomaticState()

        userStateMachine.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            reminderScheduler.cancelReminders()
            reminderScheduler.scheduleCheckInReminder(any())
        }
    }

    @Test
    fun `diagnose - when state is not changed, does not reschedule the reminder`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns DefaultState

        userStateMachine.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            reminderScheduler wasNot Called
        }
    }

    @Test
    fun `diagnose - adds recovery message to inbox when new state is default`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns DefaultState

        userStateMachine.diagnose(LocalDate.now().minusDays(10), nonEmptySetOf(COUGH))

        verify {
            userInbox.addRecovery()
        }
    }

    @Test
    fun `diagnose - does not add recovery message when state is not default`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnose(any(), any(), any()) } returns buildSymptomaticState()

        userStateMachine.diagnose(LocalDate.now(), nonEmptySetOf(COUGH))

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `diagnoseCheckIn - delegates to transitions with current state and symptoms`() {
        val symptoms = nonEmptySetOf(COUGH, TEMPERATURE)
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns DefaultState

        userStateMachine.diagnoseCheckIn(symptoms)

        verify {
            transitions.diagnoseForCheckin(DefaultState, symptoms)
        }
    }

    @Test
    fun `diagnoseCheckIn - saves the new state`() {
        every { userStateStorage.get() } returns DefaultState
        val state = buildSymptomaticState()
        every { transitions.diagnoseForCheckin(any(), any()) } returns state

        userStateMachine.diagnoseCheckIn(setOf(TEMPERATURE))

        verify {
            userStateStorage.set(state)
        }
    }

    @Test
    fun `diagnoseCheckIn - when new state is default, adds recovery message to inbox`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns DefaultState

        userStateMachine.diagnoseCheckIn(setOf(COUGH))

        verify {
            userInbox.addRecovery()
        }
    }

    @Test
    fun `diagnoseCheckIn - when new state is default but user has no symptoms, does not add recovery message`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns DefaultState

        userStateMachine.diagnoseCheckIn(emptySet())

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `diagnoseCheckIn - when new state is not default, does not add recovery message`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.diagnoseForCheckin(any(), any()) } returns buildSymptomaticState()

        userStateMachine.diagnoseCheckIn(setOf(TEMPERATURE))

        verify {
            userInbox wasNot Called
        }
    }

    @Test
    fun `transitionOnExpiredExposedState - delegates to transitions`() {
        val state = buildExposedState()
        every { userStateStorage.get() } returns state
        every { transitions.transitionOnExpiredExposedState(any()) } returns DefaultState

        userStateMachine.transitionOnExpiredExposedState()

        verify {
            transitions.transitionOnExpiredExposedState(state)
        }
    }

    @Test
    fun `transitionOnExpiredExposedState - saves the new state`() {
        val state = buildExposedState()
        every { userStateStorage.get() } returns state
        every { transitions.transitionOnExpiredExposedState(any()) } returns state

        userStateMachine.transitionOnExpiredExposedState()

        verify {
            userStateStorage.set(state)
        }
    }

    @Test
    fun `transitionOnExposure - delegates to transition with current state and exposure date`() {
        val exposureDate = DateTime.now()
        val state = buildSymptomaticState()
        every { userStateStorage.get() } returns state
        every { transitions.transitionOnExposure(any(), any()) } returns DefaultState

        userStateMachine.transitionOnExposure(exposureDate)

        verify {
            transitions.transitionOnExposure(state, exposureDate)
        }
    }

    @Test
    fun `transitionOnExposure - saves the new state`() {
        every { userStateStorage.get() } returns DefaultState
        val state = buildExposedState()
        every { transitions.transitionOnExposure(any(), any()) } returns state

        userStateMachine.transitionOnExposure(DateTime.now())

        verify {
            userStateStorage.set(state)
        }
    }

    @Test
    fun `transitionOnExposure - executes callback the when state is changed`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.transitionOnExposure(any(), any()) } returns buildExposedState()

        val onStateChanged = mockk<() -> Unit>(relaxed = true)

        userStateMachine.transitionOnExposure(DateTime.now(), onStateChanged)

        verify {
            onStateChanged.invoke()
        }
    }

    @Test
    fun `transitionOnExposure - when state is changed, cancels current reminder and schedules a new one for the new state`() {
        every { userStateStorage.get() } returns DefaultState
        val newState = buildExposedState()
        every { transitions.transitionOnExposure(any(), any()) } returns newState

        val onStateChanged = mockk<() -> Unit>(relaxed = true)

        userStateMachine.transitionOnExposure(DateTime.now(), onStateChanged)

        verify {
            reminderScheduler.cancelReminders()
            reminderScheduler.scheduleExpiredExposedReminder(newState.until)
        }
    }

    @Test
    fun `transitionOnExposure - when state is not changed, does not execute the callback`() {
        val state = buildSymptomaticState()
        every { userStateStorage.get() } returns state
        every { transitions.transitionOnExposure(any(), any()) } returns state

        val onStateChanged = mockk<() -> Unit>()

        userStateMachine.transitionOnExposure(DateTime.now(), onStateChanged)

        verify {
            onStateChanged wasNot Called
        }
    }

    @Test
    fun `transitionOnExposure - when state is not changed, does not reschedule the reminder`() {
        val state = buildSymptomaticState()
        every { userStateStorage.get() } returns state
        every { transitions.transitionOnExposure(any(), any()) } returns state

        val onStateChanged = mockk<() -> Unit>()

        userStateMachine.transitionOnExposure(DateTime.now(), onStateChanged)

        verify {
            reminderScheduler wasNot Called
        }
    }

    @Test
    fun `transitionOnTestResult - delegates to transitions with current state and test info`() {
        val state = buildPositiveState()
        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime.now())
        every { userStateStorage.get() } returns state
        every { transitions.transitionOnTestResult(any(), any()) } returns DefaultState

        userStateMachine.transitionOnTestResult(testInfo)

        verify {
            transitions.transitionOnTestResult(state, testInfo)
        }
    }

    @Test
    fun `transitionOnTestResult - when state is changed, saves the new state`() {
        every { userStateStorage.get() } returns DefaultState
        val state = buildSymptomaticState()
        every { transitions.transitionOnTestResult(any(), any()) } returns state

        userStateMachine.transitionOnTestResult(TestInfo(TestResult.POSITIVE, DateTime.now()))

        verify {
            userStateStorage.set(state)
        }
    }

    @Test
    fun `transitionOnTestResult - when state is not changed, does not update the state storage`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.transitionOnTestResult(any(), any()) } returns DefaultState

        userStateMachine.transitionOnTestResult(TestInfo(TestResult.POSITIVE, DateTime.now()))

        verify(exactly = 0) {
            userStateStorage.set(any())
        }
    }

    @Test
    fun `transitionOnTestResult - when state is changed, cancels current reminder and schedules a new one for the new state`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.transitionOnTestResult(any(), any()) } returns buildSymptomaticState()

        userStateMachine.transitionOnTestResult(TestInfo(TestResult.POSITIVE, DateTime.now()))

        verify {
            reminderScheduler.cancelReminders()
            reminderScheduler.scheduleCheckInReminder(any())
        }
    }

    @Test
    fun `transitionOnTestResult - when state is not changed, does not reschedule the reminder`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.transitionOnTestResult(any(), any()) } returns DefaultState

        userStateMachine.transitionOnTestResult(TestInfo(TestResult.POSITIVE, DateTime.now()))

        verify {
            reminderScheduler wasNot Called
        }
    }

    @Test
    fun `transitionOnTestResult - adds test info message in inbox`() {
        every { userStateStorage.get() } returns DefaultState
        every { transitions.transitionOnTestResult(any(), any()) } returns DefaultState

        val testInfo = TestInfo(TestResult.POSITIVE, DateTime.now())
        userStateMachine.transitionOnTestResult(testInfo)

        verify {
            userInbox.addTestInfo(testInfo)
        }
    }

    @Test
    fun `hasAnyOfMainSymptoms - with cough, temperature or loss of smell`() {
        assertThat(userStateMachine.hasAnyOfMainSymptoms(setOf(COUGH))).isTrue()
        assertThat(userStateMachine.hasAnyOfMainSymptoms(setOf(TEMPERATURE))).isTrue()
        assertThat(userStateMachine.hasAnyOfMainSymptoms(setOf(ANOSMIA))).isTrue()
    }

    @Test
    fun `hasAnyOfMainSymptoms - with anything other than cough, temperature or loss of smell`() {
        assertThat(
            userStateMachine.hasAnyOfMainSymptoms(
                setOf(
                    Symptom.NAUSEA,
                    Symptom.SNEEZE
                )
            )
        ).isFalse()
    }
}
