/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test

class MessageAcknowledgeTest {
    private val ackDao = mockk<AcknowledgmentsDao>(relaxUnitFun = true)
    private val ackApi = mockk<AcknowledgmentsApi>(relaxUnitFun = true)

    private val messageAcknowledge = MessageAcknowledge(ackDao, ackApi)

    @Test
    fun `hasBeenAcknowledged - returns false if message was not acknowledged before`() {
        every { ackDao.tryFind(any()) } returns null

        val message = DummyNotificationMessage("::a url::")

        assertThat(messageAcknowledge.hasBeenAcknowledged(message)).isFalse()
    }

    @Test
    fun `hasBeenAcknowledged - returns false if message has no acknowledgmentUrl`() {
        val message = DummyNotificationMessage(null)

        assertThat(messageAcknowledge.hasBeenAcknowledged(message)).isFalse()

        verifyAll {
            ackDao wasNot Called
        }
    }

    @Test
    fun `hasBeenAcknowledged - returns true if message has been acknowledged before`() {
        every { ackDao.tryFind(any()) } returns Acknowledgment("::a url::")

        val message = DummyNotificationMessage("::a url::")

        assertThat(messageAcknowledge.hasBeenAcknowledged(message)).isTrue()
    }

    @Test
    fun `acknowledgeIfNecessary - when message has no acknowledgmentUrl`() {
        val message = DummyNotificationMessage(null)

        assertThat(messageAcknowledge.acknowledgeIfNecessary(message))

        verifyAll {
            ackDao wasNot Called
            ackApi wasNot Called
        }
    }

    @Test
    fun `acknowledgeIfNecessary - when message has an acknowledgmentUrl`() {
        val message = DummyNotificationMessage("::a url::")

        assertThat(messageAcknowledge.acknowledgeIfNecessary(message))

        verifyAll {
            ackApi.send("::a url::")
            ackDao.insert(Acknowledgment("::a url::"))
        }
    }

    data class DummyNotificationMessage(override val acknowledgmentUrl: String?) :
        NotificationMessage() {
        override fun handle() = Unit
    }
}
