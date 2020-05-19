/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationChannels.Channel.ContactAndCheckin
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.AtRiskActivity
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import javax.inject.Inject

class NotificationHandler @Inject constructor(
    private val sender: NotificationSender,
    private val userStateStorage: UserStateStorage,
    private val activationCodeProvider: ActivationCodeProvider,
    private val registrationManager: RegistrationManager,
    private val acknowledgmentsDao: AcknowledgmentsDao,
    private val acknowledgmentsApi: AcknowledgmentsApi,
    private val sonarIdProvider: SonarIdProvider,
    private val tokenRefreshWorkScheduler: TokenRefreshWorkScheduler
) {

    fun handleNewToken(token: String) {
        if (!sonarIdProvider.hasProperSonarId())
            return

        tokenRefreshWorkScheduler.schedule(sonarIdProvider.get(), token)
    }

    fun handleNewMessage(messageData: Map<String, String>) {
        val wasHandled = hasBeenAcknowledged(messageData)

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
                            showStatusNotification()
                        }
                }
            }
        }

        acknowledgeIfNecessary(messageData)
    }

    private fun showStatusNotification() {
        sender.send(
            ContactAndCheckin,
            NOTIFICATION_SERVICE_ID,
            R.string.notification_title,
            R.string.notification_text,
            AtRiskActivity.Companion::getIntent
        )
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
                acknowledgmentsDao.insert(acknowledgment)
            }

    private fun isContactAlert(data: Map<String, String>) =
        data[TYPE_KEY] == TYPE_STATUS_UPDATE && data[STATUS_KEY] == STATUS_POTENTIAL

    private fun isActivation(data: Map<String, String>) =
        data.containsKey(ACTIVATION_CODE_KEY)

    companion object {
        private const val TYPE_KEY = "type"
        private const val TYPE_STATUS_UPDATE = "Status Update"
        private const val STATUS_KEY = "status"
        private const val STATUS_POTENTIAL = "Potential"
        private const val ACTIVATION_CODE_KEY = "activationCode"
        private const val ACKNOWLEDGMENT_URL = "acknowledgmentUrl"
        private const val NOTIFICATION_SERVICE_ID = 10001
    }
}
