package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import com.polidea.rxandroidble2.RxBleClient
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.AppDatabase
import uk.nhs.nhsx.sonar.android.app.ble.BleEventTracker
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.DefaultSaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.LongLiveConnectionScan
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.Scan
import uk.nhs.nhsx.sonar.android.app.ble.Scanner
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventV2Dao
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothCryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.Encrypter
import uk.nhs.nhsx.sonar.android.app.crypto.EphemeralKeyProvider
import uk.nhs.nhsx.sonar.android.app.di.ApplicationComponent
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule.Companion.ENCRYPT_SONAR_ID
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule.Companion.USE_CONNECTION_V2
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.NotificationsModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.registration.TokenRetriever
import uk.nhs.nhsx.sonar.android.app.status.StatusStorage
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import uk.nhs.nhsx.sonar.android.client.security.ServerPublicKeyProvider
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        EncryptionKeyStorageModule::class,
        NotificationsModule::class,
        TestModule::class
    ]
)
interface TestAppComponent : ApplicationComponent

@Module
class TestModule(
    appContext: Context,
    private val rxBleClient: RxBleClient,
    private val startTimestampProvider: () -> DateTime,
    private val endTimestampProvider: () -> DateTime,
    private val connectionV2: Boolean = true,
    private val encryptSonarId: Boolean = false
) {

    private val bluetoothModule = BluetoothModule(appContext, connectionV2, encryptSonarId)
    private val persistenceModule = PersistenceModule(appContext)
    private val cryptoModule = CryptoModule()

    @Provides
    fun provideTokenRetriever(): TokenRetriever =
        TestTokenRetriever()

    @Provides
    fun providePostCodeProvider(): PostCodeProvider =
        persistenceModule.providePostCodeProvider()

    @Provides
    fun provideBluetoothManager(): BluetoothManager =
        bluetoothModule.provideBluetoothManager()

    @Provides
    fun provideBluetoothAdvertiser(bluetoothManager: BluetoothManager): BluetoothLeAdvertiser =
        bluetoothModule.provideBluetoothAdvertiser(bluetoothManager)

    @Provides
    fun provideRxBleClient(): RxBleClient =
        rxBleClient

    @Provides
    fun provideDatabase() = persistenceModule.provideDatabase()

    @Provides
    fun provideContactEventDao(database: AppDatabase): ContactEventDao =
        persistenceModule.provideContactEventDao(database)

    @Provides
    fun provideSonarIdProvider(): SonarIdProvider =
        persistenceModule.provideSonarIdProvider()

    @Provides
    fun provideOnboardingStatusProvider(): OnboardingStatusProvider =
        persistenceModule.provideOnboardingStatusProvider()

    @Provides
    fun providesStatusStorage(): StatusStorage =
        persistenceModule.providesStatusStorage()

    @Provides
    fun provideEphemeralKeyProvider(): EphemeralKeyProvider = cryptoModule.provideEphemeralKeyProvider()

    @Provides
    fun provideServerPublicKeyProvider(): ServerPublicKeyProvider =
        cryptoModule.provideServerPublicKeyProvider()

    @Provides
    fun providesEncrypter(
        serverPublicKeyProvider: ServerPublicKeyProvider,
        ephemeralKeyProvider: EphemeralKeyProvider
    ): Encrypter =
        cryptoModule.provideEncrypter(serverPublicKeyProvider, ephemeralKeyProvider)

    @Provides
    fun provideBluetoothCryptogramProvider(
        sonarIdProvider: SonarIdProvider,
        encrypter: Encrypter
    ): BluetoothCryptogramProvider =
        cryptoModule.provideBluetoothCryptogramProvider(sonarIdProvider, encrypter)

    @Provides
    fun provideContactEventV2Dao(database: AppDatabase): ContactEventV2Dao {
        return database.contactEventV2Dao()
    }

    @Provides
    fun provideBleEventTracker(): BleEvents =
        BleEventTracker()

    @Provides
    fun provideSaveContactWorker(
        contactEventDao: ContactEventDao,
        contactEventV2Dao: ContactEventV2Dao,
        @Named(AppModule.DISPATCHER_IO) ioDispatcher: CoroutineDispatcher
    ): SaveContactWorker =
        DefaultSaveContactWorker(
            ioDispatcher,
            contactEventDao,
            contactEventV2Dao,
            startTimestampProvider
        )

    @Provides
    fun provideScanner(
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
            Scan(rxBleClient, saveContactWorker, bleEvents)

    @Provides
    @Named(USE_CONNECTION_V2)
    fun provideUseConnectionV2() = connectionV2

    @Provides
    @Named(ENCRYPT_SONAR_ID)
    fun provideEncryptSonarId() = encryptSonarId
}
