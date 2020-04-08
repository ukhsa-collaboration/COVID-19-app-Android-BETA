/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.app.Application
import android.app.Service
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.colocate.di.ApplicationComponent
import com.example.colocate.di.DaggerApplicationComponent
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.RegistrationModule
import com.example.colocate.di.module.StatusModule
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.polidea.rxandroidble2.exceptions.BleException
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule

const val BASE_URL = "https://sonar-colocate-services-test.apps.cp.data.england.nhs.uk"

class ColocateApplication : Application() {

    lateinit var appComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerApplicationComponent.builder()
            .persistenceModule(PersistenceModule(this))
            .bluetoothModule(BluetoothModule(this, connectionV2 = true))
            .appModule(AppModule(this))
            .networkModule(NetworkModule(BASE_URL))
            .encryptionKeyStorageModule(EncryptionKeyStorageModule(this))
            .statusModule(StatusModule(this))
            .registrationModule(RegistrationModule())
            .build()

        RxJavaPlugins.setErrorHandler { throwable ->
            if (throwable is UndeliverableException && throwable.cause is BleException) {
                return@setErrorHandler // ignore BleExceptions as they were surely delivered at least once
            }
            // add other custom handlers if needed
            throw RuntimeException("Unexpected Throwable in RxJavaPlugins error handler", throwable)
        }

        FirebaseApp.initializeApp(this)

        when (BuildConfig.BUILD_TYPE) {
            "staging" -> {
                Timber.plant(Timber.DebugTree())
            }
            "debug" -> {
                Timber.plant(Timber.DebugTree())
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
            }
        }
    }
}

val AppCompatActivity.appComponent: ApplicationComponent
    get() = (application as ColocateApplication).appComponent

val Service.appComponent: ApplicationComponent
    get() = (application as ColocateApplication).appComponent

fun Context.showShortToast(stringResource: Int) =
    Toast.makeText(this, getString(stringResource), Toast.LENGTH_SHORT).show()

fun Context.showLongToast(stringResource: Int) =
    Toast.makeText(this, getString(stringResource), Toast.LENGTH_LONG).show()
