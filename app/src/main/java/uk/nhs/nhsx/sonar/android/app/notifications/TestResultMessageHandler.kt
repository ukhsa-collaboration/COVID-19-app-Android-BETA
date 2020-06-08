/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.status.UserStateMachine
import javax.inject.Inject

class TestResultMessageHandler @Inject constructor(
    private val userStateMachine: UserStateMachine,
    private val testResultNotification: TestResultNotification
) {

    fun handle(message: TestResultMessage) {

        val testInfo = TestInfo(message.result, message.date)

        userStateMachine.transitionOnTestResult(testInfo)
        testResultNotification.show()
    }
}
