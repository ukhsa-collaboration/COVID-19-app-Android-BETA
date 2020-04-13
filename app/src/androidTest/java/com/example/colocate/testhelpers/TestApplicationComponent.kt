package com.example.colocate.testhelpers

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import com.example.colocate.ble.BleEventTracker
import com.example.colocate.ble.BleEvents
import com.example.colocate.ble.DefaultSaveContactWorker
import com.example.colocate.ble.LongLiveConnectionScan
import com.example.colocate.ble.SaveContactWorker
import com.example.colocate.ble.Scan
import com.example.colocate.ble.Scanner
import com.example.colocate.crypto.Encrypter
import com.example.colocate.crypto.EphemeralKeyProvider
import com.example.colocate.di.ApplicationComponent
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.BluetoothModule.Companion.ENCRYPT_SONAR_ID
import com.example.colocate.di.module.BluetoothModule.Companion.USE_CONNECTION_V2
import com.example.colocate.di.module.CryptoModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.NotificationsModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.StatusModule
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.BluetoothCryptogramProvider
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ContactEventV2Dao
import com.example.colocate.persistence.SonarIdProvider
import com.example.colocate.registration.TokenRetriever
import com.polidea.rxandroidble2.RxBleClient
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import uk.nhs.nhsx.sonar.android.client.security.ServerPublicKeyProvider
import java.util.Date
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        EncryptionKeyStorageModule::class,
        StatusModule::class,
        NotificationsModule::class,
        TestModule::class
    ]
)
interface TestAppComponent : ApplicationComponent

@Module
class TestModule(
    appContext: Context,
    private val rxBleClient: RxBleClient,
    private val startTimestampProvider: () -> Date,
    private val endTimestampProvider: () -> Date,
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
