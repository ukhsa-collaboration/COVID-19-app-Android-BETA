/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Period
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ble.Identifier
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptogramProvider @Inject constructor(
    private val sonarIdProvider: SonarIdProvider,
    private val encrypter: Encrypter,
    private val cryptogramStorage: CryptogramStorage,
    private val currentDateProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) }
) {
    private val lock = Object()
    private var cachedDate: DateTime? = null
    private var cachedCryptogram: Cryptogram? = null

    fun canProvideCryptogram(): Boolean =
        sonarIdProvider.hasProperSonarId() && encrypter.canEncrypt()

    fun provideCryptogram(): Cryptogram {
        synchronized(lock) {
            val currentDate = currentDateProvider()
            if (cachedDate != null && cachedCryptogram != null) {
                if (currentCryptogramExpired(cachedDate!!, currentDate)) {
                    generateAndStoreCryptogram(currentDate)
                } else {
                    return cachedCryptogram!!
                }
            }

            val (storedLatestDate, storedCryptogram) = cryptogramStorage.get()
            if (storedLatestDate == -1L) {
                return generateAndStoreCryptogram(currentDate)
            }

            return if (currentCryptogramExpired(DateTime(storedLatestDate), currentDate)) {
                generateAndStoreCryptogram(currentDate)
            } else {
                if (storedCryptogram == null) {
                    Timber.e("Stored validity date without having stored cryptogram!")
                    throw java.lang.IllegalStateException("Stored validity date without having stored cryptogram!")
                }
                updateCache(currentDate, storedCryptogram)
                storedCryptogram
            }
        }
    }

    private fun generateAndStoreCryptogram(
        validityDate: DateTime
    ): Cryptogram {
        val cryptogram = generateCryptogram(validityDate)
        updateStorage(validityDate, cryptogram)
        return cryptogram
    }

    private fun updateStorage(
        validityDate: DateTime,
        cryptogram: Cryptogram
    ) {
        cryptogramStorage.set(Pair(validityDate.millis, cryptogram))
        updateCache(validityDate, cryptogram)
    }

    private fun updateCache(validityDate: DateTime, cryptogram: Cryptogram) {
        cachedDate = validityDate
        cachedCryptogram = cryptogram
    }

    private fun currentCryptogramExpired(latestDate: DateTime, currentDate: DateTime) =
        currentDate.isAfter(latestDate.startOfNextDay()) || currentDate.isBefore(latestDate)

    private fun generateCryptogram(latestDate: DateTime): Cryptogram {
        val encodedStartDate = latestDate.withTimeAtStartOfDay().encodeAsSecondsSinceEpoch()
        val encodedEndDate = latestDate.startOfNextDay().encodeAsSecondsSinceEpoch()
        val residentIdBytes = Identifier.fromString(sonarIdProvider.get()).asBytes
        return encrypter.encrypt(encodedStartDate, encodedEndDate, residentIdBytes, COUNTRY_CODE)
    }

    private fun DateTime.startOfNextDay(): DateTime =
        this.plus(Period.days(1)).withTimeAtStartOfDay()
}
