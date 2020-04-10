package com.example.colocate.notifications

import com.example.colocate.R
import com.example.colocate.registration.ActivationCodeObserver
import com.example.colocate.status.AtRiskActivity
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage

class NotificationHandler(
    private val sender: NotificationSender,
    private val statusStorage: StatusStorage,
    private val activationCodeObserver: ActivationCodeObserver
) {

    fun handle(messageData: Map<String, String>) {
        when {
            isActivation(messageData) -> {
                val activationCode = messageData[ACTIVATION_CODE_KEY]!!
                activationCodeObserver.onGetActivationCode(activationCode)
            }
            isStatusUpdate(messageData) -> {
                statusStorage.update(CovidStatus.POTENTIAL)
                showStatusNotification()
            }
        }
    }

    private fun showStatusNotification() {
        sender.send(
            NOTIFICATION_SERVICE_ID,
            R.string.notification_title,
            R.string.notification_text,
            AtRiskActivity.Companion::getIntent
        )
    }

    private fun isStatusUpdate(data: Map<String, String>) =
        data.containsKey(STATUS_KEY)

    private fun isActivation(data: Map<String, String>) =
        data.containsKey(ACTIVATION_CODE_KEY)

    companion object {
        private const val STATUS_KEY = "status"
        private const val ACTIVATION_CODE_KEY = "activationCode"
        private const val NOTIFICATION_SERVICE_ID = 10001
    }
}
