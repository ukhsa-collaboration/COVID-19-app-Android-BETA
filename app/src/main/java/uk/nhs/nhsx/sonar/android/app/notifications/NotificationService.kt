/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class NotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Timber.d("Received new token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.i("New Message: ${message.messageId} ${message.data}")
//        notificationHandler.handle(message.data)
    }
}
