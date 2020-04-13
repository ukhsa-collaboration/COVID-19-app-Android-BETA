/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.crypto

import com.example.colocate.ble.Identifier
import com.example.colocate.persistence.BluetoothCryptogramProvider
import com.example.colocate.persistence.SonarIdProvider
import uk.nhs.nhsx.sonar.android.client.colocation.Seconds
import java.util.Date

class ConcreteBluetoothCryptogramProvider(
    private val sonarIdProvider: SonarIdProvider,
    private val encrypter: Encrypter
) : BluetoothCryptogramProvider {
    private lateinit var latestDate: Date
    private var cryptogram: Cryptogram? = null
    private val lock = Object()

    // TODO: Parametrize, preferably via something we inject.
    private val offset: Seconds = 24 * 60 * 60

    override fun provideBluetoothCryptogram(): Cryptogram {
        synchronized(lock) {
            val currentDate = Date()
            return if (currentCryptogramExpired(currentDate)) {
                cryptogram = generateCryptogram()
                latestDate = currentDate
                cryptogram!!
            } else {
                cryptogram!!
            }
        }
    }

    override fun canProvideCryptogram(): Boolean {
        // TODO: Ensure encrypter has server public key
        return sonarIdProvider.hasProperSonarId()
    }

    private fun currentCryptogramExpired(currentDate: Date): Boolean {
        val expiryDate = Date(latestDate.time + offset * 1_000)
        return currentDate.after(expiryDate)
    }

    private fun generateCryptogram(): Cryptogram {
        val encodedStartDate = latestDate.encodeAsSecondsSinceEpoch()
        val encodedEndDate = latestDate.encodeAsSecondsSinceEpoch(offset)
        val residentIdBytes = Identifier.fromString(sonarIdProvider.getSonarId()).asBytes
        return encrypter.encrypt(encodedStartDate + encodedEndDate + residentIdBytes)
    }
}
