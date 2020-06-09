/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.UserStateMachine

class ExposureMessageHandlerTest {

    private val exposedNotification = mockk<ExposedNotification>(relaxUnitFun = true)
    private val userStateStorage = mockk<UserStateMachine>(relaxUnitFun = true)
    private val handler = ExposureMessageHandler(userStateStorage, exposedNotification)

    @Test
    fun `handle exposure message`() {
        val date = DateTime("2020-04-23T18:34:00Z")

        val message = ExposureMessage(
            handler,
            acknowledgmentUrl = "::a url::",
            date = date
        )

        handler.handle(message)

        verify(exactly = 1) {
            userStateStorage.transitionOnExposure(
                exposureDate = date,
                onStateChanged = any()
            )
        }
    }
}
