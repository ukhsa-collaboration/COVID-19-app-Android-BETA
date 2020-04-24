/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.work.ListenableWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.polidea.rxandroidble2.exceptions.BleException
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import net.danlew.android.joda.JodaTimeAndroid
import org.bouncycastle.jce.provider.BouncyCastleProvider
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.contactevents.DeleteOutdatedEvents
import uk.nhs.nhsx.sonar.android.app.crypto.PROVIDER_NAME
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.DaggerApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.NotificationsModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import java.security.Security

class ColocateApplication : Application() {

    lateinit var appComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        JodaTimeAndroid.init(this)

        configureBouncyCastleProvider()

        appComponent = buildApplicationComponent()

        configureRxJavaErrorHandler()

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

        DeleteOutdatedEvents.schedule(this)
    }

    private fun configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler { throwable ->
            if (throwable is UndeliverableException && throwable.cause is BleException) {
                return@setErrorHandler // ignore BleExceptions as they were surely delivered at least once
            }
            // add other custom handlers if needed
            throw RuntimeException("Unexpected Throwable in RxJavaPlugins error handler", throwable)
        }
    }

    private fun buildApplicationComponent(): ApplicationComponent =
        DaggerApplicationComponent.builder()
            .appModule(AppModule(this))
            .persistenceModule(PersistenceModule(this))
            .bluetoothModule(
                BluetoothModule(
                    this,
                    scanIntervalLength = 8
                )
            )
            .cryptoModule(CryptoModule())
            .networkModule(NetworkModule(BuildConfig.BASE_URL, BuildConfig.SONAR_HEADER_VALUE))
            .notificationsModule(NotificationsModule())
            .build()

    private fun configureBouncyCastleProvider() {
        // Remove existing built in Bouncy Castle
        Security.removeProvider(PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}

val ListenableWorker.appComponent: ApplicationComponent
    get() = (applicationContext as ColocateApplication).appComponent

val Context.appComponent: ApplicationComponent
    get() = (applicationContext as ColocateApplication).appComponent

fun Context.showToast(@StringRes stringResource: Int, duration: Int = Toast.LENGTH_LONG) =
    Toast.makeText(this, getString(stringResource), duration).show()
