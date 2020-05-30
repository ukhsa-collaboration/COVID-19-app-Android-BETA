/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import javax.inject.Inject

class TestResultMessageHandler @Inject constructor(
    private val reminders: Reminders,
    private val userStateStorage: UserStateStorage,
    private val userInbox: UserInbox,
    private val testResultNotification: TestResultNotification
) {

    fun handle(message: TestResultMessage) {

        val testInfo = TestInfo(message.result, message.date)

        userStateStorage.get()
            .let { state -> UserStateTransitions.transitionOnTestResult(state, testInfo) }
            .let { state ->
                userStateStorage.set(state)
                state.scheduleCheckInReminder(reminders)

                userInbox.addTestInfo(testInfo)
                testResultNotification.show()
            }
    }
}
