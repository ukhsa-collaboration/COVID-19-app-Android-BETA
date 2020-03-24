package com.example.colocate.di

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import androidx.room.Room
import com.example.colocate.ble.BluetoothService
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ResidentIdProvider
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import java.util.UUID
import javax.inject.Named

@Component(modules = [PersistenceModule::class, AppModule::class, BluetoothModule::class, NetworkModule::class])
interface ApplicationComponent {
    fun inject(bluetoothService: BluetoothService)
    fun inject(bluetoothService: IsolateActivity)
}

@Module
class BluetoothModule(private val applicationContext: Context) {
    @Provides
    fun provideBluetoothManager(): BluetoothManager =
        getSystemService(applicationContext, BluetoothManager::class.java)!!

    @Provides
    fun provideBluetoothScanner(bluetoothManager: BluetoothManager) =
        bluetoothManager.adapter.bluetoothLeScanner

    @Provides
    fun provideBluetoothAdvertiser(bluetoothManager: BluetoothManager) =
        bluetoothManager.adapter.bluetoothLeAdvertiser
}

@Module
class AppModule(private val applicationContext: Context) {
    @Provides
    fun provideContext() = applicationContext

    @Provides
    @Named(DISPATCHER_MAIN)
    fun mainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Named(DISPATCHER_IO)
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    companion object {
        const val DISPATCHER_MAIN = "DISPATCHER_MAIN"
        const val DISPATCHER_IO = "DISPATCHER_IO"
    }
}

@Module
class PersistenceModule(private val applicationContext: Context) {
    @Provides
    fun provideDatabase() =
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "event-database"
        ).build()

    @Provides
    fun provideContactEventDao(database: AppDatabase): ContactEventDao {
        return database.contactEventDao()
    }

    @Provides
    fun provideResidentIdProvider(): ResidentIdProvider {
        return object : ResidentIdProvider {
            override fun getResidentId(): UUID {
                return UUID.randomUUID()
            }
        }
    }
}

@Module
class NetworkModule(private val baseUrl: String) {
    @Provides
    fun provideHttpClient(context: Context): HttpClient = VolleyHttpClient(baseUrl, context)
}
