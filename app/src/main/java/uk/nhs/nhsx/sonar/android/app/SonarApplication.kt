/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.app.Application
import android.content.Context
import androidx.work.ListenableWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.polidea.rxandroidble2.exceptions.BleException
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import net.danlew.android.joda.JodaTimeAndroid
import org.bouncycastle.jce.provider.BouncyCastleProvider
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.analytics.AppCenterAnalytics
import uk.nhs.nhsx.sonar.android.app.contactevents.DeleteOutdatedEventsWorker
import uk.nhs.nhsx.sonar.android.app.crypto.PROVIDER_NAME
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.DaggerApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.NotificationsModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.util.AndroidLocationHelper
import uk.nhs.nhsx.sonar.android.app.util.registerShakeDetector
import java.security.KeyStore
import java.security.Security

class SonarApplication : Application() {

    lateinit var appComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        JodaTimeAndroid.init(this)

        configureBouncyCastleProvider()

        startAnalytics()
        appComponent = buildApplicationComponent()

        configureRxJavaErrorHandler()

        FirebaseApp.initializeApp(this)

        when (BuildConfig.BUILD_TYPE) {
            "staging" -> {
                Timber.plant(Timber.DebugTree())
                registerShakeDetector()
            }
            "debug" -> {
                Timber.plant(Timber.DebugTree())
                logFirebaseToken()
                registerShakeDetector()
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
            }
        }

        DeleteOutdatedEventsWorker.schedule(this)
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
            .appModule(AppModule(this, AndroidLocationHelper(this), AppCenterAnalytics()))
            .persistenceModule(PersistenceModule(this))
            .bluetoothModule(BluetoothModule(this, scanIntervalLength = 8))
            .cryptoModule(
                CryptoModule(
                    this,
                    KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                )
            )
            .networkModule(NetworkModule(BuildConfig.BASE_URL, BuildConfig.SONAR_HEADER_VALUE))
            .notificationsModule(NotificationsModule())
            .build()

    private fun startAnalytics() {
        AppCenter.start(
            this,
            BuildConfig.SONAR_ANALYTICS_KEY,
            Analytics::class.java
        )
    }

    private fun configureBouncyCastleProvider() {
        // Remove existing built in Bouncy Castle
        Security.removeProvider(PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}

val ListenableWorker.appComponent: ApplicationComponent
    get() = (applicationContext as SonarApplication).appComponent

val Context.appComponent: ApplicationComponent
    get() = (applicationContext as SonarApplication).appComponent
