/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.notifications

import com.example.colocate.appComponent
import com.example.colocate.registration.ActivationCodeObserver
import com.example.colocate.status.StatusStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import javax.inject.Inject

class NotificationService : FirebaseMessagingService() {

    @Inject
    protected lateinit var statusStorage: StatusStorage

    @Inject
    lateinit var activationCodeObserver: ActivationCodeObserver

    override fun onCreate() {
        appComponent.inject(this)
    }

    override fun onNewToken(token: String) {
        Timber.d("Received new token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.i("New Message: ${message.messageId} ${message.data}")

        val handler = NotificationHandler(
            AndroidNotificationSender(this),
            statusStorage,
            activationCodeObserver
        )

        handler.handle(message.data)
    }
}
