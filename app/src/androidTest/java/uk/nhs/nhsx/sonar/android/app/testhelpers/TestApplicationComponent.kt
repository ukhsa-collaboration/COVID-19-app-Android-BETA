/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.content.Context
import com.polidea.rxandroidble2.RxBleClient
import dagger.Component
import dagger.Module
import dagger.Provides
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.AppDatabase
import uk.nhs.nhsx.sonar.android.app.DeviceDetection
import uk.nhs.nhsx.sonar.android.app.analytics.AnalyticEvent
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.Scan
import uk.nhs.nhsx.sonar.android.app.ble.Scanner
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        PersistenceModule::class,
        BluetoothModule::class,
        CryptoModule::class,
        NetworkModule::class,
        TestNotificationsModule::class,
        TestAnalyticsModule::class
    ]
)
interface TestAppComponent : ApplicationComponent {
    fun getSonarIdProvider(): SonarIdProvider
    fun getKeyStorage(): KeyStorage
    fun getAppDatabase(): AppDatabase
    fun getUserStateStorage(): UserStateStorage
    fun getOnboardingStatusProvider(): OnboardingStatusProvider
    fun getActivationCodeProvider(): ActivationCodeProvider
    fun getReferenceCodeProvider(): ReferenceCodeProvider
}

class TestBluetoothModule(
    private val appContext: Context,
    private val rxBleClient: RxBleClient,
    private val currentTimestampProvider: () -> DateTime,
    private val scanIntervalLength: Int = 2
) : BluetoothModule(appContext, scanIntervalLength) {

    override fun provideRxBleClient(): RxBleClient =
        rxBleClient

    override fun provideScanner(
        rxBleClient: RxBleClient,
        saveContactWorker: SaveContactWorker,
        bleEvents: BleEvents
    ): Scanner =
        Scan(
            rxBleClient,
            saveContactWorker,
            bleEvents,
            currentTimestampProvider,
            scanIntervalLength
        )

    var simulateUnsupportedDevice = false

    override fun provideDeviceDetection(): DeviceDetection {
        return if (simulateUnsupportedDevice) {
            DeviceDetection(null, appContext.packageManager)
        } else {
            super.provideDeviceDetection()
        }
    }

    fun reset() {
        simulateUnsupportedDevice = false
    }
}

@Module
class TestNotificationsModule {

    @Provides
    fun provideTokenRetriever(): TokenRetriever =
        TestTokenRetriever()
}

@Module
class TestAnalyticsModule {
    @Provides
    fun provideAnalytics(): SonarAnalytics =
        TestAnalytics()
}

class TestAnalytics : SonarAnalytics {
    override fun trackEvent(event: AnalyticEvent) {
        // Do nothing
    }
}
