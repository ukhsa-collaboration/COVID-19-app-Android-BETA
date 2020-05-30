/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.PositiveState
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions

class TestResultMessageHandlerTest {

    private val testResultNotification = mockk<TestResultNotification>(relaxUnitFun = true)
    private val userStateStorage = mockk<UserStateStorage>(relaxUnitFun = true)
    private val userInbox = mockk<UserInbox>()
    private val reminders = mockk<Reminders>(relaxUnitFun = true)

    private val handler = TestResultMessageHandler(
        reminders,
        userStateStorage,
        userInbox,
        testResultNotification
    )

    @Test
    fun `handle test result message`() {
        val message = TestResultMessage(
            handler,
            acknowledgmentUrl = "::a url::",
            result = TestResult.POSITIVE,
            date = DateTime("2020-04-23T18:34:00Z")
        )

        val testInfo = TestInfo(message.result, message.date)

        every { userStateStorage.get() } returns DefaultState
        every { userInbox.addTestInfo(testInfo) } returns Unit

        handler.handle(message)

        verify {
            userStateStorage.get()

            UserStateTransitions.transitionOnTestResult(DefaultState, testInfo)

            userStateStorage.set(any<PositiveState>())

            reminders.cancelCheckinReminder()
            reminders.scheduleCheckInReminder(any())

            userInbox.addTestInfo(testInfo)
            testResultNotification.show()
        }
    }
}
