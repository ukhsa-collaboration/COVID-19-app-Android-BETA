/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.colocate.status.CovidStatus
import com.example.colocate.status.StatusStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import javax.inject.Inject

class RegistrationNotificationService : FirebaseMessagingService() {

    @Inject
    protected lateinit var residentApi: ResidentApi

    @Inject
    protected lateinit var statusStorage: StatusStorage

    override fun onCreate() {
        (applicationContext as ColocateApplication).applicationComponent.inject(this)
    }

    override fun onNewToken(token: String) {
        Timber.d("Received new token: $token")
        residentApi.register(token,
            { Timber.d("Registration was successful") },
            { error -> Timber.e(error, "Registration has failed") })
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.i("New Message: ${message.messageId}")

        if (isActivationMessage(message)) {
            val activationCode = message.data[ACTIVATION_CODE_KEY]!!
            residentApi.confirmDevice(activationCode,
                { registration -> Timber.d("Received registration $registration") },
                { error -> Timber.e(error, "Registration confirmation has failed !") })
        } else if (isStatusMessage(message)) {
            statusStorage.update(CovidStatus.POTENTIAL)
            startActivity(Intent(this, AtRiskActivity::class.java))
            showNotification()
        }
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(getString(R.string.notification_title))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(getString(R.string.notification_text)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_SERVICE_ID, notification)
        }
    }
    private fun isStatusMessage(message: RemoteMessage) =
        message.data.containsKey(STATUS_KEY)

    private fun isActivationMessage(message: RemoteMessage) =
        message.data.containsKey(ACTIVATION_CODE_KEY)

    companion object {
        private const val STATUS_KEY = "status"
        private const val ACTIVATION_CODE_KEY = "activationCode"
        private const val NOTIFICATION_SERVICE_ID = 10001
    }
}
