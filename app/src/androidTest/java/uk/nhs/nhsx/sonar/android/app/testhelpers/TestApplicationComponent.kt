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
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.LongLiveConnectionScan
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.Scan
import uk.nhs.nhsx.sonar.android.app.ble.Scanner
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.notifications.ReminderTimeProvider
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.ActivationCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.client.KeyStorage
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        PersistenceModule::class,
        BluetoothModule::class,
        CryptoModule::class,
        NetworkModule::class,
        TestNotificationsModule::class
    ]
)
interface TestAppComponent : ApplicationComponent {
    fun getSonarIdProvider(): SonarIdProvider
    fun getKeyStorage(): KeyStorage
    fun getAppDatabase(): AppDatabase
    fun getStateStorage(): StateStorage
    fun getOnboardingStatusProvider(): OnboardingStatusProvider
    fun getActivationCodeProvider(): ActivationCodeProvider
}

class TestBluetoothModule(
    private val appContext: Context,
    private val rxBleClient: RxBleClient,
    private val startTimestampProvider: () -> DateTime,
    private val endTimestampProvider: () -> DateTime,
    private val currentTimestampProvider: () -> DateTime,
    private val connectionV2: Boolean = false,
    // TODO: Flip this switch - requires more known cryptograms. Needs alignment on source of truth
    private val encryptSonarId: Boolean = false,
    private val scanIntervalLength: Int = 2
) : BluetoothModule(appContext, scanIntervalLength, connectionV2, encryptSonarId) {

    override fun provideRxBleClient(): RxBleClient =
        rxBleClient

    override fun provideScanner(
        rxBleClient: RxBleClient,
        saveContactWorker: SaveContactWorker,
        bleEvents: BleEvents
    ): Scanner =
        if (connectionV2)
            LongLiveConnectionScan(
                rxBleClient,
                saveContactWorker,
                startTimestampProvider,
                endTimestampProvider,
                periodInMilliseconds = 50,
                bleEvents = bleEvents
            )
        else
            Scan(
                rxBleClient,
                saveContactWorker,
                bleEvents,
                currentTimestampProvider,
                encryptSonarId,
                scanIntervalLength
            )
}

@Module
class TestNotificationsModule {

    @Provides
    fun provideTokenRetriever(): TokenRetriever =
        TestTokenRetriever()

    @Provides
    fun provideReminderTimeProvider(): ReminderTimeProvider =
        TestReminderTimeProvider()
}
