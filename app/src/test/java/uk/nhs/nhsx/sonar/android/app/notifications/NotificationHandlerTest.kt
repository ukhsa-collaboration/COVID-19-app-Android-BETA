/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult

class NotificationHandlerTest {

    private val exposureMessageHandler = mockk<ExposureMessageHandler>(relaxed = true)
    private val activationCodeMessageHandler = mockk<ActivationCodeMessageHandler>()
    private val testResultMessageHandler = mockk<TestResultMessageHandler>(relaxed = true)
    private val messageAcknowledge = mockk<MessageAcknowledge>(relaxed = true)

    private val handler = NotificationHandler(
        exposureMessageHandler,
        activationCodeMessageHandler,
        testResultMessageHandler,
        messageAcknowledge
    )

    @Test
    fun `test handleNewMessage - unrecognized message`() {
        val messageData = mapOf("foo" to "bar")

        handler.handleNewMessage(messageData)

        verifyAll {
            exposureMessageHandler wasNot Called
            testResultMessageHandler wasNot Called
            activationCodeMessageHandler wasNot Called
        }
    }

    @Test
    fun `test handleNewMessage - activation code message`() {
        val messageData = mapOf("activationCode" to "::a code::")

        every { activationCodeMessageHandler.handle(any()) } returns Unit

        handler.handleNewMessage(messageData)

        verify {
            activationCodeMessageHandler.handle(
                ActivationCodeMessage(
                    activationCodeMessageHandler,
                    null,
                    "::a code::"
                )
            )
        }
    }

    @Test
    fun `test handleNewMessage - exposure message without exposure date (legacy)`() {
        val messageData = mapOf(
            "type" to "Status Update",
            "status" to "Potential"
        )

        val slot = io.mockk.slot<ExposureMessage>()
        handler.handleNewMessage(messageData)

        verify {
            exposureMessageHandler.handle(capture(slot))
        }

        assertThat(slot.captured.date).isBetween(DateTime.now().minusSeconds(1), DateTime.now())
        assertThat(slot.captured.handler).isEqualTo(exposureMessageHandler)
        assertThat(slot.captured.acknowledgmentUrl).isNull()
    }

    @Test
    fun `test handleNewMessage - exposure message`() {
        val exposureDate = "2020-04-23T18:34:00Z"
        val messageData = mapOf(
            "type" to "Status Update",
            "status" to "Potential",
            "mostRecentProximityEventDate" to exposureDate
        )

        handler.handleNewMessage(messageData)

        verify {
            exposureMessageHandler.handle(
                ExposureMessage(
                    exposureMessageHandler,
                    null,
                    DateTime(exposureDate)
                )
            )
        }
    }

    @Test
    fun `test handleNewMessage - test result`() {
        val messageData = mapOf(
            "type" to "Test Result",
            "result" to "NEGATIVE",
            "testTimestamp" to "2020-04-23T18:34:00Z"
        )

        handler.handleNewMessage(messageData)

        verify {
            testResultMessageHandler.handle(
                TestResultMessage(
                    testResultMessageHandler,
                    null,
                    TestResult.NEGATIVE,
                    DateTime("2020-04-23T18:34:00Z")
                )
            )
        }
    }

    @Test
    fun `test handleNewMessage - a notification with acknowledgmentUrl`() {
        every { messageAcknowledge.hasBeenAcknowledged(any()) } returns false
        every { activationCodeMessageHandler.handle(any()) } returns Unit

        val messageData = mapOf(
            "activationCode" to "::a code::",
            "acknowledgmentUrl" to "https://api.example.com/ack/100"
        )

        handler.handleNewMessage(messageData)

        verify {
            messageAcknowledge.acknowledgeIfNecessary(
                ActivationCodeMessage(
                    activationCodeMessageHandler,
                    "https://api.example.com/ack/100",
                    "::a code::"
                )
            )
        }
    }

    @Test
    fun `test handleNewMessage - when it has already been received`() {
        every { messageAcknowledge.hasBeenAcknowledged(any()) } returns true

        val messageData = mapOf(
            "activationCode" to "::a code::",
            "acknowledgmentUrl" to "https://api.example.com/ack/100"
        )

        handler.handleNewMessage(messageData)

        verify {
            messageAcknowledge.acknowledgeIfNecessary(
                ActivationCodeMessage(
                    activationCodeMessageHandler,
                    "https://api.example.com/ack/100",
                    "::a code::"
                )
            )

            activationCodeMessageHandler wasNot Called
        }
    }
}
