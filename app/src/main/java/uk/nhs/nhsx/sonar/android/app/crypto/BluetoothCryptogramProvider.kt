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

    private var latestDate: DateTime? = null
    private var cryptogram: Cryptogram? = null
    private val lock = Object()

    // TODO: Parametrize
    private val offset: Period = Period.hours(24)

    fun provideBluetoothCryptogram(): Cryptogram {
        synchronized(lock) {
            val currentDate = DateTime.now(DateTimeZone.UTC)
            return if (currentCryptogramExpired(currentDate)) {
                latestDate = currentDate
                cryptogram = generateCryptogram()
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
        if (latestDate == null) return true
        val expiryDate = latestDate!!.plus(offset)
        return currentDate.isAfter(expiryDate)
    }

    private fun generateCryptogram(): Cryptogram {
        val encodedStartDate = latestDate!!.encodeAsSecondsSinceEpoch()
        val encodedEndDate = latestDate!!.encodeAsSecondsSinceEpoch(offset.seconds)
        val residentIdBytes = Identifier.fromString(sonarIdProvider.getSonarId()).asBytes
        return encrypter.encrypt(encodedStartDate + encodedEndDate + residentIdBytes + byteArrayOf(0x00, 0x00))
    }
}
