/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class ExposureMessageHandlerTest {

    private val exposedNotification = mockk<ExposedNotification>(relaxUnitFun = true)
    private val userStateStorage = mockk<UserStateStorage>(relaxUnitFun = true)

    private val handler = ExposureMessageHandler(userStateStorage, exposedNotification)

    @Test
    fun `handle exposure message`() {
        val message = ExposureMessage(
            handler,
            acknowledgmentUrl = "::a url::",
            date = DateTime("2020-04-23T18:34:00Z")
        )

        val slot = slot<ExposedState>()
        every { userStateStorage.get() } returns DefaultState

        handler.handle(message)

        verifyAll {
            userStateStorage.get()
            userStateStorage.set(capture(slot))

            exposedNotification.show()
        }

        assertThat(slot.captured.since.toLocalDate()).isEqualTo(LocalDate.parse("2020-04-23"))
    }

    @Test
    fun `handle exposure message - in exposed state`() {
        val message = ExposureMessage(
            handler,
            acknowledgmentUrl = "::a url::",
            date = DateTime("2020-04-23T18:34:00Z")
        )

        every { userStateStorage.get() } returns UserState.exposed(LocalDate.now())

        handler.handle(message)

        verifyAll {
            userStateStorage.get()
            exposedNotification wasNot Called
        }
    }

    @Test
    fun `handle exposure message - in symptomatic state`() {
        val message = ExposureMessage(
            handler,
            acknowledgmentUrl = "::a url::",
            date = DateTime("2020-04-23T18:34:00Z")
        )

        every { userStateStorage.get() } returns UserState.symptomatic(
            LocalDate.now(),
            nonEmptySetOf(Symptom.COUGH)
        )

        handler.handle(message)

        verifyAll {
            userStateStorage.get()
            exposedNotification wasNot Called
        }
    }
}
