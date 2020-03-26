/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import com.example.colocate.registration.ActivationCodeObserver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import javax.inject.Inject

class RegistrationNotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var residentApi: ResidentApi

    @Inject
    lateinit var activationCodeObserver: ActivationCodeObserver

    override fun onCreate() {
        (applicationContext as ColocateApplication).applicationComponent.inject(this)
    }

    override fun onNewToken(token: String) {
        Timber.d("Received new token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("onMessageReceived $message")
        val activationCode = message.data["activationCode"] ?: return
        activationCodeObserver.onGetActivationCode(activationCode)
    }
}
