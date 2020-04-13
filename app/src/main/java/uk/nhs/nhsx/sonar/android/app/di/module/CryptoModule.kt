/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.crypto.BouncyEncrypter
import uk.nhs.nhsx.sonar.android.app.crypto.ConcreteBluetoothCryptogramProvider
import uk.nhs.nhsx.sonar.android.app.crypto.ECP256KeyProvider
import uk.nhs.nhsx.sonar.android.app.crypto.Encrypter
import uk.nhs.nhsx.sonar.android.app.crypto.EphemeralKeyProvider
import uk.nhs.nhsx.sonar.android.app.crypto.FakeServerPublicKeyProvider
import uk.nhs.nhsx.sonar.android.app.persistence.BluetoothCryptogramProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SonarIdProvider
import uk.nhs.nhsx.sonar.android.client.security.ServerPublicKeyProvider
import javax.inject.Singleton

@Module
class CryptoModule {
    @Provides
    fun provideEphemeralKeyProvider(): EphemeralKeyProvider {
        return ECP256KeyProvider()
    }

    @Provides
    fun provideServerPublicKeyProvider(): ServerPublicKeyProvider {
        return FakeServerPublicKeyProvider()
    }

    @Provides
    fun provideEncrypter(
        serverPublicKeyProvider: ServerPublicKeyProvider,
        ephemeralKeyProvider: EphemeralKeyProvider
    ): Encrypter {
        return BouncyEncrypter(serverPublicKeyProvider, ephemeralKeyProvider)
    }

    @Provides
    @Singleton
    fun provideBluetoothCryptogramProvider(
        sonarIdProvider: SonarIdProvider,
        encrypter: Encrypter
    ): BluetoothCryptogramProvider =
        ConcreteBluetoothCryptogramProvider(
            sonarIdProvider,
            encrypter
        )
}
