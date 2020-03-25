package uk.nhs.nhsx.sonar.android.client.di

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage
import uk.nhs.nhsx.sonar.android.client.security.SharedPreferencesEncryptionKeyStorage

@Module
class EncryptionKeyStorageModule(val context: Context) {
    @Provides
    fun provideEncryptionKeyStorage(): EncryptionKeyStorage {
        return SharedPreferencesEncryptionKeyStorage(context)
    }
}
