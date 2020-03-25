/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import com.google.firebase.messaging.FirebaseMessagingService
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

    override fun onNewToken(token: String) {
        Timber.i("Received new token... $httpClient")
        ResidentApi(httpClient).register(token)
    }
}
