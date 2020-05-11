/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Period
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.ble.Identifier
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import javax.inject.Inject
import javax.inject.Singleton

const val CRYPTOGRAM_FILENAME = "CRYPTOGRAM_STORAGE"
const val CRYPTOGRAM_PREF_NAME = "CRYPTOGRAM"
const val LATEST_DATE_PREF_NAME = "LATEST_DATE"

@Singleton
class CryptogramProvider @Inject constructor(
    private val sonarIdProvider: SonarIdProvider,
    private val encrypter: Encrypter,
    private val context: Context,
    private val currentDateProvider: () -> DateTime = { DateTime.now(DateTimeZone.UTC) },
    private val base64Encoder: (ByteArray) -> String = {
        Base64.encodeToString(
            it,
            Base64.DEFAULT
        )
    },
    private val base64Decoder: (String) -> ByteArray = { Base64.decode(it, Base64.DEFAULT) }
) {
    private val lock = Object()
    private var cachedDate: DateTime? = null
    private var cachedCryptogram: Cryptogram? = null

    fun provideCryptogram(): Cryptogram {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(CRYPTOGRAM_FILENAME, Context.MODE_PRIVATE)
            val currentDate = currentDateProvider()
            if (cachedDate != null && cachedCryptogram != null) {
                if (currentCryptogramExpired(cachedDate!!, currentDate)) {
                    generateAndStoreCryptogram(currentDate, prefs)
                } else {
                    return cachedCryptogram!!
                }
            }

            val storedLatestDate = prefs.getLong(LATEST_DATE_PREF_NAME, -1L)
            if (storedLatestDate == -1L) {
                return generateAndStoreCryptogram(currentDate, prefs)
            }

            return if (currentCryptogramExpired(DateTime(storedLatestDate), currentDate)) {
                generateAndStoreCryptogram(currentDate, prefs)
            } else {
                val encodedStoredCryptogram = prefs.getString(CRYPTOGRAM_PREF_NAME, "")
                if (encodedStoredCryptogram.isNullOrEmpty()) {
                    Timber.e("Stored validity date without having stored cryptogram!")
                    throw java.lang.IllegalStateException("Stored validity date without having stored cryptogram!")
                }
                val cryptogram = Cryptogram.fromBytes(base64Decoder(encodedStoredCryptogram))
                updateCache(currentDate, cryptogram)
                cryptogram
            }
        }
    }

    private fun generateAndStoreCryptogram(
        validityDate: DateTime,
        prefs: SharedPreferences
    ): Cryptogram {
        val cryptogram = generateCryptogram(validityDate)
        updateStorage(prefs, validityDate, cryptogram)
        return cryptogram
    }

    private fun updateStorage(
        prefs: SharedPreferences,
        validityDate: DateTime,
        cryptogram: Cryptogram
    ) {
        updateSharedPreferences(prefs, validityDate, cryptogram)
        updateCache(validityDate, cryptogram)
    }

    private fun updateCache(validityDate: DateTime, cryptogram: Cryptogram) {
        cachedDate = validityDate
        cachedCryptogram = cryptogram
    }

    private fun updateSharedPreferences(
        prefs: SharedPreferences,
        validityDate: DateTime,
        cryptogram: Cryptogram
    ) {
        prefs.edit()
            .putLong(LATEST_DATE_PREF_NAME, validityDate.millis)
            .putString(CRYPTOGRAM_PREF_NAME, base64Encoder(cryptogram.asBytes()))
            .apply()
    }

    private fun currentCryptogramExpired(latestDate: DateTime, currentDate: DateTime): Boolean {
        val expiryDate = latestDate.startOfNextDay()
        return currentDate.isAfter(expiryDate) || currentDate.isBefore(latestDate)
    }

    private fun generateCryptogram(latestDate: DateTime): Cryptogram {
        val encodedStartDate = latestDate.withTimeAtStartOfDay().encodeAsSecondsSinceEpoch()
        val encodedEndDate = latestDate.startOfNextDay().encodeAsSecondsSinceEpoch()
        val residentIdBytes = Identifier.fromString(sonarIdProvider.get()).asBytes
        return encrypter.encrypt(encodedStartDate, encodedEndDate, residentIdBytes, COUNTRY_CODE)
    }

    private fun DateTime.startOfNextDay(): DateTime =
        this.plus(Period.days(1)).withTimeAtStartOfDay()

    fun canProvideCryptogram(): Boolean =
        sonarIdProvider.hasProperSonarId() && encrypter.canEncrypt()
}
