/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Period
import uk.nhs.nhsx.sonar.android.app.ble.Identifier
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothIdProvider @Inject constructor(
    private val sonarIdProvider: SonarIdProvider,
    private val encrypter: Encrypter,
    private val currentDateProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) }
) {

    private var latestDate: DateTime? = null
    private var cryptogram: Cryptogram? = null
    private val countryCode = byteArrayOf(
        'G'.toByte(),
        'B'.toByte()
    )
    private val txPowerLevel = (-7).toByte()

    private val lock = Object()

    fun provideBluetoothPayload(): BluetoothIdentifier {
        synchronized(lock) {
            val currentDate = currentDateProvider()
            if (currentCryptogramExpired(currentDate)) {
                latestDate = currentDate
                cryptogram = generateCryptogram()
            }
            return BluetoothIdentifier(countryCode, cryptogram!!, txPowerLevel)
        }
    }

    // TODO: Ensure encrypter has server public key
    fun canProvideCryptogram(): Boolean {
        return sonarIdProvider.hasProperSonarId()
    }

    private fun currentCryptogramExpired(currentDate: DateTime): Boolean {
        if (latestDate == null) return true
        val expiryDate = latestDate!!.startOfNextDay()
        return currentDate.isAfter(expiryDate)
    }

    private fun generateCryptogram(): Cryptogram {
        if (latestDate == null) {
            throw IllegalStateException("Cannot generate cryptogram without latestDate being set.")
        }
        val encodedStartDate = latestDate!!.withTimeAtStartOfDay().encodeAsSecondsSinceEpoch()
        val encodedEndDate = latestDate!!.startOfNextDay().encodeAsSecondsSinceEpoch()
        val residentIdBytes = Identifier.fromString(sonarIdProvider.getSonarId()).asBytes
        return encrypter.encrypt(encodedStartDate, encodedEndDate, residentIdBytes, countryCode)
    }

    private fun DateTime.startOfNextDay(): DateTime =
        this.plus(Period.days(1)).withTimeAtStartOfDay()
}
