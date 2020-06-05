/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import javax.inject.Inject

class ExposureMessageHandler @Inject constructor(
    private val userStateStorage: UserStateStorage,
    private val exposedNotification: ExposedNotification
) {
    fun handle(message: ExposureMessage) {
        userStateStorage.transitionOnContactAlert(
            date = message.date,
            onStateChanged = { exposedNotification.show() }
        )
    }
}
