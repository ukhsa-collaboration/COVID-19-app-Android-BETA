/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.notifications

import com.example.colocate.appComponent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import javax.inject.Inject

class NotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHandler: NotificationHandler

    override fun onCreate() {
        appComponent.inject(this)
    }

    override fun onNewToken(token: String) {
        Timber.d("Received new token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.i("New Message: ${message.messageId} ${message.data}")
        notificationHandler.handle(message.data)
    }
}
