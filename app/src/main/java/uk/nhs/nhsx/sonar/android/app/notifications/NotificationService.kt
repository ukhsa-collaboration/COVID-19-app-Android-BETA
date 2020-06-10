/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.appComponent
import javax.inject.Inject

class NotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHandler: NotificationHandler

    @Inject
    lateinit var newTokenHandler: NewTokenHandler

    override fun onCreate() {
        appComponent.inject(this)
    }

    override fun onNewToken(token: String) {
        Timber.d("Received new token: $token")
        newTokenHandler.handle(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("New Message: ${message.messageId} ${message.data}")
        notificationHandler.handleNewMessage(message.data)
    }
}
