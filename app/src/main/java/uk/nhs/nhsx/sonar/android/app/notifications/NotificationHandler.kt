/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeObserver
import uk.nhs.nhsx.sonar.android.app.status.AtRiskActivity
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.client.AcknowledgementsApi
import javax.inject.Inject

class NotificationHandler @Inject constructor(
    private val sender: NotificationSender,
    private val stateStorage: StateStorage,
    private val activationCodeObserver: ActivationCodeObserver,
    private val acknowledgementsDao: AcknowledgementsDao,
    private val acknowledgementsApi: AcknowledgementsApi
) {

    fun handle(messageData: Map<String, String>) {
        val wasHandled = hasBeenAcknowledged(messageData)

        if (!wasHandled) {
            when {
                isActivation(messageData) -> {
                    val activationCode = messageData.getValue(ACTIVATION_CODE_KEY)
                    activationCodeObserver.onGetActivationCode(activationCode)
                }
                isStatusUpdate(messageData) -> {
                    stateStorage.update(EmberState(DateTime.now(DateTimeZone.UTC).plusDays(14)))
                    showStatusNotification()
                }
            }
        }

        acknowledgeIfNecessary(messageData)
    }

    private fun showStatusNotification() {
        sender.send(
            NOTIFICATION_SERVICE_ID,
            R.string.notification_title,
            R.string.notification_text,
            AtRiskActivity.Companion::getIntent
        )
    }

    private fun hasBeenAcknowledged(data: Map<String, String>) =
        data[ACKNOWLEDGEMENT_URL]
            ?.let { url -> acknowledgementsDao.tryFind(url) != null }
            ?: false

    private fun acknowledgeIfNecessary(data: Map<String, String>) =
        data[ACKNOWLEDGEMENT_URL]
            ?.let { url ->
                val acknowledgement = Acknowledgement(url)
                acknowledgementsApi.send(acknowledgement.url)
                acknowledgementsDao.insert(acknowledgement)
            }

    private fun isStatusUpdate(data: Map<String, String>) =
        data.containsKey(STATUS_KEY)

    private fun isActivation(data: Map<String, String>) =
        data.containsKey(ACTIVATION_CODE_KEY)

    companion object {
        private const val STATUS_KEY = "status"
        private const val ACTIVATION_CODE_KEY = "activationCode"
        private const val ACKNOWLEDGEMENT_URL = "acknowledgementUrl"
        private const val NOTIFICATION_SERVICE_ID = 10001
    }
}
