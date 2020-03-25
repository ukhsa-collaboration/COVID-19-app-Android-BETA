/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import javax.inject.Inject

class RegistrationNotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var httpClient: HttpClient

    override fun onCreate() {
        (applicationContext as ColocateApplication).applicationComponent.inject(this)
    }

    private val residentApi = ResidentApi(httpClient)

    override fun onNewToken(token: String) {
        Timber.d("Received new token: $token")
        residentApi.register(token,
            { Timber.d("Registration was successful") },
            { error -> Timber.e(error, "Registration has failed") })
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val activationCode = message.data["activationCode"] ?: return
        residentApi.confirmDevice(activationCode,
            { registration -> Timber.d("Received registration $registration") },
            { error -> Timber.e(error, "Registration confirmation has failed !") })
    }
}
