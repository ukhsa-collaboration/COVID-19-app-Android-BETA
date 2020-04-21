/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeObserver
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.client.AcknowledgmentsApi

class NotificationHandlerTest {

    private val sender = mockk<NotificationSender>(relaxUnitFun = true)
    private val statusStorage = mockk<StateStorage>(relaxUnitFun = true)
    private val activationCodeObserver = mockk<ActivationCodeObserver>(relaxUnitFun = true)
    private val ackDao = mockk<AcknowledgmentsDao>(relaxUnitFun = true)
    private val ackApi = mockk<AcknowledgmentsApi>(relaxUnitFun = true)
    private val handler = NotificationHandler(sender, statusStorage, activationCodeObserver, ackDao, ackApi)

    @Test
    fun testOnMessageReceived_UnrecognizedNotification() {
        val messageData = mapOf("foo" to "bar")

        handler.handle(messageData)

        verifyAll {
            sender wasNot Called
            statusStorage wasNot Called
            activationCodeObserver wasNot Called
        }
    }

    @Test
    fun testOnMessageReceived_Activation() {
        val messageData = mapOf("activationCode" to "code-023")

        handler.handle(messageData)

        verify { activationCodeObserver.onGetActivationCode("code-023") }
    }

    @Test
    fun testOnMessageReceived_StatusUpdate() {
        val messageData = mapOf("status" to "POTENTIAL")

        handler.handle(messageData)

        verifyAll {
            statusStorage.update(any<EmberState>())
            sender.send(10001, R.string.notification_title, R.string.notification_text, any())
        }
    }

    @Test
    fun testOnMessageReceived_WithAcknowledgmentUrl() {
        every { ackDao.tryFind(any()) } returns null

        val messageData = mapOf("status" to "POTENTIAL", "acknowledgmentUrl" to "https://api.example.com/ack/100")

        handler.handle(messageData)

        verifyAll {
            statusStorage.update(any<EmberState>())
            sender.send(10001, R.string.notification_title, R.string.notification_text, any())
            ackApi.send("https://api.example.com/ack/100")
            ackDao.tryFind("https://api.example.com/ack/100")
            ackDao.insert(Acknowledgment("https://api.example.com/ack/100"))
        }
    }

    @Test
    fun testOnMessageReceived_WhenItHasAlreadyBeenReceived() {
        every { ackDao.tryFind(any()) } returns Acknowledgment("https://api.example.com/ack/101")

        val messageData = mapOf("status" to "POTENTIAL", "acknowledgmentUrl" to "https://api.example.com/ack/101")

        handler.handle(messageData)

        verifyAll {
            statusStorage wasNot Called
            sender wasNot Called
            ackApi.send("https://api.example.com/ack/101")
            ackDao.tryFind("https://api.example.com/ack/101")
            ackDao.insert(Acknowledgment("https://api.example.com/ack/101"))
        }
    }
}
