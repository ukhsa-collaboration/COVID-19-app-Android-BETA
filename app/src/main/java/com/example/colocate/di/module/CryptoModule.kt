/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import com.example.colocate.crypto.BouncyEncrypter
import com.example.colocate.crypto.ConcreteBluetoothCryptogramProvider
import com.example.colocate.crypto.ECP256KeyProvider
import com.example.colocate.crypto.Encrypter
import com.example.colocate.crypto.EphemeralKeyProvider
import com.example.colocate.crypto.FakeServerPublicKeyProvider
import com.example.colocate.persistence.BluetoothCryptogramProvider
import com.example.colocate.persistence.SonarIdProvider
import dagger.Module
import dagger.Provides
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
