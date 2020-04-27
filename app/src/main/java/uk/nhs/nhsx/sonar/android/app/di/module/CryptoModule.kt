/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.crypto.BluetoothIdProvider
import uk.nhs.nhsx.sonar.android.app.crypto.Encrypter
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.SharedPreferencesKeyStorage
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider

@Module
class CryptoModule {

    @Provides
    fun provideEncryptionKeyStorage(implementation: SharedPreferencesKeyStorage): KeyStorage =
        implementation

    @Provides
    fun provideBluetoothCryptogramProvider(
        sonarIdProvider: SonarIdProvider,
        encrypter: Encrypter
    ): BluetoothIdProvider =
        BluetoothIdProvider(sonarIdProvider, encrypter)
}
