/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import javax.inject.Inject

class NotificationHandler @Inject constructor(
    private val userStateStorage: UserStateStorage,
    private val userInbox: UserInbox,
    private val activationCodeProvider: ActivationCodeProvider,
    private val registrationManager: RegistrationManager,
    private val acknowledgmentsDao: AcknowledgmentsDao,
    private val acknowledgmentsApi: AcknowledgmentsApi,
    private val sonarIdProvider: SonarIdProvider,
    private val exposedNotification: ExposedNotification,
    private val testResultNotification: TestResultNotification,
    private val tokenRefreshWorkScheduler: TokenRefreshWorkScheduler
) {

    fun handleNewToken(token: String) {
        if (!sonarIdProvider.hasProperSonarId())
            return

        tokenRefreshWorkScheduler.schedule(sonarIdProvider.get(), token)
    }

    fun handleNewMessage(messageData: Map<String, String>) {
        val wasHandled = hasBeenAcknowledged(messageData)

        // TODO: Should we persist the push notification before processing it?

        if (!wasHandled) {
            when {
                isActivation(messageData) -> {
                    val activationCode = messageData.getValue(ACTIVATION_CODE_KEY)
                    activationCodeProvider.set(activationCode)
                    registrationManager.register()
                }
                isContactAlert(messageData) -> {
                    userStateStorage.get()
                        .let { UserStateTransitions.transitionOnContactAlert(it) }
                        ?.let {
                            userStateStorage.set(it)
                            exposedNotification.show()
                        }
                }
                isTestResult(messageData) -> {
                    val testInfo = TestInfo(
                        TestResult.valueOf(messageData.getValue(TEST_RESULT_KEY)),
                        DateTime(messageData.getValue(TEST_RESULT_DATE_KEY))
                    )

                    userStateStorage.get()
                        .let { currentState ->
                            UserStateTransitions.transitionOnTestResult(currentState, testInfo)
                        }
                        .let {
                            userStateStorage.set(it)
                            userInbox.addTestInfo(testInfo)
                            testResultNotification.show()
                        }
                }
            }
        }

        acknowledgeIfNecessary(messageData)
    }

    private fun hasBeenAcknowledged(data: Map<String, String>) =
        data[ACKNOWLEDGMENT_URL]
            ?.let { url -> acknowledgmentsDao.tryFind(url) != null }
            ?: false

    private fun acknowledgeIfNecessary(data: Map<String, String>) =
        data[ACKNOWLEDGMENT_URL]
            ?.let { url ->
                val acknowledgment = Acknowledgment(url)
                acknowledgmentsApi.send(acknowledgment.url)
                // TODO: Check for 200 response before persisting acknowledgement
                acknowledgmentsDao.insert(acknowledgment)
            }

    private fun isContactAlert(data: Map<String, String>) =
        data[TYPE_KEY] == TYPE_STATUS_UPDATE && data[STATUS_KEY] == STATUS_POTENTIAL

    private fun isActivation(data: Map<String, String>) =
        data.containsKey(ACTIVATION_CODE_KEY)

    private fun isTestResult(data: Map<String, String>) =
        data[TYPE_KEY] == TYPE_TEST_RESULT &&
            data.containsKey(TEST_RESULT_KEY) &&
            data.containsKey(TEST_RESULT_DATE_KEY)

    companion object {
        private const val TYPE_KEY = "type"
        private const val TYPE_STATUS_UPDATE = "Status Update"
        private const val TYPE_TEST_RESULT = "Test Result"
        private const val STATUS_KEY = "status"
        private const val STATUS_POTENTIAL = "Potential"
        private const val TEST_RESULT_KEY = "result"
        private const val TEST_RESULT_DATE_KEY = "testTimestamp"
        private const val ACTIVATION_CODE_KEY = "activationCode"
        private const val ACKNOWLEDGMENT_URL = "acknowledgmentUrl"
    }
}
