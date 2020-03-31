package com.example.colocate

import com.example.colocate.di.ApplicationComponent
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.StatusModule
import com.example.colocate.registration.TokenRetriever
import dagger.Component
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PersistenceModule::class,
        AppModule::class,
        BluetoothModule::class,
        NetworkModule::class,
        EncryptionKeyStorageModule::class,
        StatusModule::class,
        TestModule::class
    ]
)
interface TestAppComponent : ApplicationComponent

@Module
class TestModule {

    @Provides
    fun provideTokenRetriever(): TokenRetriever = TestTokenRetriever()
}
