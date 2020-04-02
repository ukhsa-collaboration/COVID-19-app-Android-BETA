package com.example.colocate

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import com.example.colocate.ble.SaveContactWorker
import com.example.colocate.ble.Scan
import com.example.colocate.ble.Scanner
import com.example.colocate.di.ApplicationComponent
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.StatusModule
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ContactEventV2Dao
import com.example.colocate.persistence.ResidentIdProvider
import com.example.colocate.registration.TokenRetriever
import com.polidea.rxandroidble2.RxBleClient
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
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
        TestModule::class
    ]
)
interface TestAppComponent : ApplicationComponent

@Module
class TestModule(
    appContext: Context,
    private val rxBleClient: RxBleClient,
    private val dateProvider: () -> Date,
    private val connectionV2: Boolean = false
) {

    private val bluetoothModule = BluetoothModule(appContext)
    private val persistenceModule = PersistenceModule(appContext, connectionV2 = connectionV2)

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
    fun provideResidentIdProvider(): ResidentIdProvider =
        persistenceModule.provideResidentIdProvider()

    @Provides
    fun provideContactEventV2Dao(database: AppDatabase): ContactEventV2Dao {
        return database.contactEventV2Dao()
    }

    @Provides
    fun provideSaveContactWorker(
        contactEventDao: ContactEventDao,
        contactEventV2Dao: ContactEventV2Dao,
        @Named(AppModule.DISPATCHER_IO) ioDispatcher: CoroutineDispatcher
    ): SaveContactWorker =
        SaveContactWorker(ioDispatcher, contactEventDao, contactEventV2Dao, dateProvider)

    @Provides
    fun provideScanner(
        rxBleClient: RxBleClient,
        saveContactWorker: SaveContactWorker
    ): Scanner {
        return Scan(rxBleClient, saveContactWorker)
    }

    @Provides
    @Named(PersistenceModule.USE_CONNECTION_V2)
    fun provideUseConnectionV2() = connectionV2
}