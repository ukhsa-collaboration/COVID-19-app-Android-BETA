package com.example.colocate.notifications

import com.example.colocate.R
import com.example.colocate.registration.ActivationCodeObserver
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NotificationHandlerTest {

    private val sender = mockk<NotificationSender>(relaxUnitFun = true)
    private val statusStorage = mockk<StatusStorage>(relaxUnitFun = true)
    private val activationCodeObserver = mockk<ActivationCodeObserver>(relaxUnitFun = true)
    private val handler = NotificationHandler(sender, statusStorage, activationCodeObserver)

    @Test
    fun testOnMessageReceived_UnrecognizedNotification() {
        val messageData = mapOf("foo" to "bar")

        handler.handle(messageData)

        verify {
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

        verify { statusStorage.update(CovidStatus.POTENTIAL) }
        verify { sender.send(10001, R.string.notification_title, R.string.notification_text, any()) }
    }
}
