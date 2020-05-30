/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import javax.inject.Inject

class ExposureMessageHandler @Inject constructor(
    private val userStateStorage: UserStateStorage,
    private val exposedNotification: ExposedNotification
) {
    fun handle(message: ExposureMessage) {
        userStateStorage.get()
            .let {
                UserStateTransitions.transitionOnContactAlert(
                    it,
                    message.date
                )
            }
            ?.let {
                userStateStorage.set(it)
                exposedNotification.show()
            }
    }
}
