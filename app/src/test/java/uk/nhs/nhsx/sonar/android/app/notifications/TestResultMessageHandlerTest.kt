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
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage

class TestResultMessageHandlerTest {

    private val testResultNotification = mockk<TestResultNotification>(relaxUnitFun = true)
    private val userStateStorage = mockk<UserStateStorage>(relaxUnitFun = true)

    private val handler = TestResultMessageHandler(
        userStateStorage,
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

        every { userStateStorage.transitionOnTestResult(testInfo) } returns Unit

        handler.handle(message)

        verify {
            userStateStorage.transitionOnTestResult(testInfo)
            testResultNotification.show()
        }
    }
}
