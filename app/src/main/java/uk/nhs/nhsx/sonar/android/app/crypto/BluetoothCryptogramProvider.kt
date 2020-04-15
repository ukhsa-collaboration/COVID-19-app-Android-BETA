/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Period
import uk.nhs.nhsx.sonar.android.app.ble.Identifier
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothCryptogramProvider @Inject constructor(
    private val sonarIdProvider: SonarIdProvider,
    private val encrypter: Encrypter
) {

    private lateinit var latestDate: DateTime
    private var cryptogram: Cryptogram? = null
    private val lock = Object()

    // TODO: Parametrize
    private val offset: Period = Period.hours(24)

    fun provideBluetoothCryptogram(): Cryptogram {
        synchronized(lock) {
            val currentDate = DateTime.now(DateTimeZone.UTC)
            return if (currentCryptogramExpired(currentDate)) {
                cryptogram = generateCryptogram()
                latestDate = currentDate
                cryptogram!!
            } else {
                cryptogram!!
            }
        }
    }

    // TODO: Ensure encrypter has server public key
    fun canProvideCryptogram(): Boolean {
        return sonarIdProvider.hasProperSonarId()
    }

    private fun currentCryptogramExpired(currentDate: DateTime): Boolean {
        val expiryDate = latestDate.plus(offset)
        return currentDate.isAfter(expiryDate)
    }

    private fun generateCryptogram(): Cryptogram {
        val encodedStartDate = latestDate.encodeAsSecondsSinceEpoch()
        val encodedEndDate = latestDate.encodeAsSecondsSinceEpoch(offset.seconds)
        val residentIdBytes = Identifier.fromString(sonarIdProvider.getSonarId()).asBytes
        return encrypter.encrypt(encodedStartDate + encodedEndDate + residentIdBytes)
    }
}
