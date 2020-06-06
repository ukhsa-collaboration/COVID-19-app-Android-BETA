/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import javax.inject.Inject

class TestResultMessageHandler @Inject constructor(
    private val userStateStorage: UserStateStorage,
    private val testResultNotification: TestResultNotification
) {

    fun handle(message: TestResultMessage) {

        val testInfo = TestInfo(message.result, message.date)

        userStateStorage.transitionOnTestResult(testInfo)
        testResultNotification.show()
    }
}
