package uk.nhs.nhsx.sonar.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.crypto.FakeServerPublicKeyProvider
import uk.nhs.nhsx.sonar.android.app.crypto.ServerPublicKeyProvider
import uk.nhs.nhsx.sonar.android.client.EncryptionKeyStorage
import uk.nhs.nhsx.sonar.android.client.SharedPreferencesEncryptionKeyStorage

@Module
class CryptoModule {

    @Provides
    fun provideServerPublicKeyProvider(implementation: FakeServerPublicKeyProvider): ServerPublicKeyProvider =
        implementation

    @Provides
    fun provideEncryptionKeyStorage(implementation: SharedPreferencesEncryptionKeyStorage): EncryptionKeyStorage =
        implementation
}
