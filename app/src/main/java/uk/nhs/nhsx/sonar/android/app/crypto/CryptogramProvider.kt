/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Interval
import org.joda.time.Minutes
import org.joda.time.Period
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
    private val validityInterval: (DateTime) -> Interval = {
        Interval(
            it,
            it.plus(Minutes.minutes(15))
        )
    }

    fun canProvideCryptogram(): Boolean =
        sonarIdProvider.hasProperSonarId() && encrypter.canEncrypt()

    fun provideCryptogram(): Cryptogram {
        synchronized(lock) {
            val currentDate = currentDateProvider()
            if (cachedDate != null && cachedCryptogram != null) {
                return if (currentCryptogramValid(cachedDate!!, currentDate)) {
                    cachedCryptogram!!
                } else {
                    generateAndStoreCryptogram(currentDate)
                }
            }

            val (storedLatestDate, storedCryptogram) = cryptogramStorage.get()
            if (storedLatestDate == -1L ||
                storedCryptogram == null ||
                !currentCryptogramValid(
                    DateTime(storedLatestDate),
                    currentDate
                )
            ) {
                return generateAndStoreCryptogram(currentDate)
            }

            updateCache(currentDate, storedCryptogram)
            return storedCryptogram
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

    private fun currentCryptogramValid(latestDate: DateTime, currentDate: DateTime) =
        validityInterval(latestDate).contains(currentDate)

    private fun generateCryptogram(latestDate: DateTime): Cryptogram {
        val validityInterval = validityInterval(latestDate)
        val encodedStartDate = validityInterval.start.encodeAsSecondsSinceEpoch()
        val encodedEndDate = validityInterval.end.encodeAsSecondsSinceEpoch()
        val residentIdBytes = Identifier.fromString(sonarIdProvider.get()).asBytes
        return encrypter.encrypt(encodedStartDate, encodedEndDate, residentIdBytes, COUNTRY_CODE)
    }

    private fun DateTime.startOfNextDay(): DateTime =
        this.plus(Period.days(1)).withTimeAtStartOfDay()
}
