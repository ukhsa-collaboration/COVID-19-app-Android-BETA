/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.app.Application
import android.app.Service
import androidx.appcompat.app.AppCompatActivity
import com.example.colocate.di.ApplicationComponent
import com.example.colocate.di.DaggerApplicationComponent
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.RegistrationModule
import com.example.colocate.di.module.StatusModule
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule

const val BASE_URL = "https://sonar-colocate-services-test.apps.cp.data.england.nhs.uk"

class ColocateApplication : Application() {

    lateinit var appComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        val useConnectionV2 = true
        appComponent = DaggerApplicationComponent.builder()
            .persistenceModule(PersistenceModule(this, useConnectionV2))
            .bluetoothModule(BluetoothModule(this))
            .appModule(AppModule(this))
            .networkModule(NetworkModule(BASE_URL))
            .encryptionKeyStorageModule(EncryptionKeyStorageModule(this))
            .statusModule(StatusModule(this))
            .registrationModule(RegistrationModule())
            .build()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

val AppCompatActivity.appComponent: ApplicationComponent
    get() = (application as ColocateApplication).appComponent

val Service.appComponent: ApplicationComponent
    get() = (application as ColocateApplication).appComponent
