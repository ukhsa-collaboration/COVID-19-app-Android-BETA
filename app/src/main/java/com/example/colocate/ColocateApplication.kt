/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.colocate.di.ApplicationComponent
import com.example.colocate.di.DaggerApplicationComponent
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.RegistrationModule
import com.example.colocate.di.module.StatusModule
import com.example.colocate.registration.RegistrationResult
import com.example.colocate.registration.RegistrationWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import java.util.concurrent.TimeUnit

const val BASE_URL = "https://sonar-colocate-services.apps.cp.data.england.nhs.uk"

class ColocateApplication : Application() {

    lateinit var applicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        applicationComponent = DaggerApplicationComponent.builder()
            .persistenceModule(PersistenceModule(this))
            .bluetoothModule(BluetoothModule(this))
            .appModule(AppModule(this))
            .networkModule(NetworkModule(BASE_URL))
            .encryptionKeyStorageModule(EncryptionKeyStorageModule(this))
            .statusModule(StatusModule(this))
            .registrationModule(RegistrationModule())
            .build()

        GlobalScope.launch {
            val result = applicationComponent.registrationUseCase().register()
            Timber.d("Registration result: $result")
            if (result == RegistrationResult.FAILURE) {
                enqueueRegistration()
            }
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun enqueueRegistration() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val registerRequest = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this@ColocateApplication).enqueue(registerRequest)
    }
}
