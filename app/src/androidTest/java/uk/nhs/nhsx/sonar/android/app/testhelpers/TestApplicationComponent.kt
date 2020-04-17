package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.content.Context
import com.polidea.rxandroidble2.RxBleClient
import dagger.Component
import dagger.Module
import dagger.Provides
import org.joda.time.DateTime
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
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        PersistenceModule::class,
        BluetoothModule::class,
        CryptoModule::class,
        NetworkModule::class,
        TestModule::class
    ]
)
interface TestAppComponent : ApplicationComponent

class TestBluetoothModule(
    private val appContext: Context,
    private val rxBleClient: RxBleClient,
    private val startTimestampProvider: () -> DateTime,
    private val endTimestampProvider: () -> DateTime,
    private val currentTimestampProvider: () -> DateTime,
    private val connectionV2: Boolean = false,
    private val encryptSonarId: Boolean = false
) : BluetoothModule(appContext, 60, connectionV2, encryptSonarId) {

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
                false
            )
}

@Module
class TestModule {

    @Provides
    fun provideTokenRetriever(): TokenRetriever =
        TestTokenRetriever()

    @Provides
    fun provideReminderTimeProvider(): ReminderTimeProvider =
        TestReminderTimeProvider()
}
