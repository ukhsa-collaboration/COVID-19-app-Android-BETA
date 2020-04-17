/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.client.KeyStorage
import uk.nhs.nhsx.sonar.android.client.SharedPreferencesKeyStorage

@Module
class CryptoModule {

    @Provides
    fun provideEncryptionKeyStorage(implementation: SharedPreferencesKeyStorage): KeyStorage =
        implementation
}
