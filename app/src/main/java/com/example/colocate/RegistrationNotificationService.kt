/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi

class RegistrationNotificationService : FirebaseMessagingService() {
    companion object {
        const val TAG = "RegistrationNotificationService"
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "Received new token...")
        ResidentApi(VolleyHttpClient("http://foo", this)).register(token)
    }
}
