/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationChannels.Channel.ContactAndCheckin
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class NotificationHandlerTest {

    private val sender = mockk<NotificationSender>(relaxUnitFun = true)
    private val userStateStorage = mockk<UserStateStorage>(relaxUnitFun = true)
    private val userInbox = mockk<UserInbox>(relaxUnitFun = true)
    private val activationCodeProvider = mockk<ActivationCodeProvider>(relaxUnitFun = true)
    private val registrationManager = mockk<RegistrationManager>(relaxUnitFun = true)
    private val ackDao = mockk<AcknowledgmentsDao>(relaxUnitFun = true)
    private val ackApi = mockk<AcknowledgmentsApi>(relaxUnitFun = true)
    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val tokenRefreshWorkScheduler = mockk<TokenRefreshWorkScheduler>(relaxUnitFun = true)
    private val handler = NotificationHandler(
        sender,
        userStateStorage,
        userInbox,
        activationCodeProvider,
        registrationManager,
        ackDao,
        ackApi,
        sonarIdProvider,
        tokenRefreshWorkScheduler
    )

    @Test
    fun `test handleNewToken - when we have a sonar id`() {
        every { sonarIdProvider.hasProperSonarId() } returns true
        every { sonarIdProvider.get() } returns "sonar-id-200"

        handler.handleNewToken("some-token #1")

        verify { tokenRefreshWorkScheduler.schedule("sonar-id-200", "some-token #1") }
    }

    @Test
    fun `test handleNewToken - when we don't have a sonar id`() {
        every { sonarIdProvider.hasProperSonarId() } returns false

        handler.handleNewToken("some-token #1")

        verify { tokenRefreshWorkScheduler wasNot Called }
    }

    @Test
    fun `test handleNewMessage - unrecognized notification`() {
        val messageData = mapOf("foo" to "bar")

        handler.handleNewMessage(messageData)

        verifyAll {
            sender wasNot Called
            userStateStorage wasNot Called
            activationCodeProvider wasNot Called
            registrationManager wasNot Called
        }
    }

    @Test
    fun `test handleNewMessage - activation`() {
        val messageData = mapOf("activationCode" to "code-023")

        handler.handleNewMessage(messageData)

        verify {
            activationCodeProvider.set("code-023")
            registrationManager.register()
        }
    }

    @Test
    fun `test handleNewMessage - status update`() {
        val messageData = mapOf(
            "type" to "Status Update",
            "status" to "Potential"
        )
        every { userStateStorage.get() } returns DefaultState

        handler.handleNewMessage(messageData)

        verifyAll {
            userStateStorage.get()
            userStateStorage.set(any<ExposedState>())
            sender.send(
                ContactAndCheckin,
                10001,
                R.string.contact_alert_notification_title,
                R.string.contact_alert_notification_text,
                any()
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

        val testInfo = TestInfo(TestResult.NEGATIVE, DateTime("2020-04-23T18:34:00Z"))

        every { userStateStorage.get() } returns DefaultState
        every { userInbox.addTestInfo(testInfo) } returns Unit

        handler.handleNewMessage(messageData)

        verify {
            userStateStorage.get()

            UserStateTransitions.transitionOnTestResult(DefaultState, testInfo)

            userStateStorage.set(any<DefaultState>())
            userInbox.addTestInfo(testInfo)

            sender.send(
                ContactAndCheckin,
                10001,
                R.string.test_result_notification_title,
                R.string.test_result_notification_text,
                any()
            )
        }
    }

    @Test
    fun `test handleNewMessage - status update in exposed state`() {
        val messageData = mapOf(
            "type" to "Status Update",
            "status" to "Potential"
        )
        every { userStateStorage.get() } returns ExposedState(DateTime.now(), DateTime.now())

        handler.handleNewMessage(messageData)

        verifyAll {
            userStateStorage.get()
            sender wasNot Called
        }
        verify(exactly = 0) { userStateStorage.set(any<ExposedState>()) }
    }

    @Test
    fun `test handleNewMessage - status update in symptomatic state`() {
        val messageData = mapOf(
            "type" to "Status Update",
            "status" to "Potential"
        )
        every { userStateStorage.get() } returns SymptomaticState(
            DateTime.now(),
            DateTime.now(),
            nonEmptySetOf(TEMPERATURE)
        )

        handler.handleNewMessage(messageData)

        verifyAll {
            userStateStorage.get()
            sender wasNot Called
        }
        verify(exactly = 0) { userStateStorage.set(any<ExposedState>()) }
    }

    @Test
    fun `test handleNewMessage - a notification with acknowledgmentUrl`() {
        every { ackDao.tryFind(any()) } returns null
        every { userStateStorage.get() } returns DefaultState

        val messageData =
            mapOf(
                "type" to "Status Update",
                "status" to "Potential",
                "acknowledgmentUrl" to "https://api.example.com/ack/100"
            )

        handler.handleNewMessage(messageData)

        verifyAll {
            userStateStorage.get()
            userStateStorage.set(any<ExposedState>())
            sender.send(
                ContactAndCheckin,
                10001,
                R.string.contact_alert_notification_title,
                R.string.contact_alert_notification_text,
                any()
            )
            ackApi.send("https://api.example.com/ack/100")
            ackDao.tryFind("https://api.example.com/ack/100")
            ackDao.insert(Acknowledgment("https://api.example.com/ack/100"))
        }
    }

    @Test
    fun `test handleNewMessage - when it has already been received`() {
        every { ackDao.tryFind(any()) } returns Acknowledgment("https://api.example.com/ack/101")

        val messageData =
            mapOf("status" to "POTENTIAL", "acknowledgmentUrl" to "https://api.example.com/ack/101")

        handler.handleNewMessage(messageData)

        verifyAll {
            userStateStorage wasNot Called
            sender wasNot Called
            ackApi.send("https://api.example.com/ack/101")
            ackDao.tryFind("https://api.example.com/ack/101")
            ackDao.insert(Acknowledgment("https://api.example.com/ack/101"))
        }
    }
}
