/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import timber.log.Timber

class SonarApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        when (BuildConfig.BUILD_TYPE) {
            "internal" -> {
                Timber.plant(Timber.DebugTree())
            }
            "debug" -> {
                Timber.plant(Timber.DebugTree())
                logFirebaseToken()
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
            }
        }
    }

    private fun logFirebaseToken() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                try {
                    if (!task.isSuccessful) {
                        Timber.d(task.exception, "FirebaseInstanceId.getInstanceId failed")
                        return@addOnCompleteListener
                    }
                    val token = task.result?.token
                    Timber.d(task.exception, "Firebase Token = $token")
                } catch (exception: Exception) {
                    Timber.e(task.exception, "Firebase Token retrieval failed")
                }
            }
    }
}
