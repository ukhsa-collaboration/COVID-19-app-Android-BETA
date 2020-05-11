/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.crypto

import android.content.Context
import android.util.Base64
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider

class CryptogramStorage(context: Context) :
    SharedPreferenceSerializingProvider<Pair<Long, Cryptogram?>>(
        context,
        "CRYPTOGRAM_STORAGE",
        "validityDateAndCryptogram",
        { (timestamp, cryptogram) ->
            val encodedCryptogram = Base64.encodeToString(
                cryptogram?.asBytes() ?: byteArrayOf(),
                Base64.DEFAULT
            )
            "$timestamp,$encodedCryptogram"
        },
        {
            if (it.isNullOrEmpty()) {
                Pair(-1, null)
            } else {
                val (timestamp, encodedCryptogram) = it.split(",")
                val cryptogram =
                    Cryptogram.fromBytes(Base64.decode(encodedCryptogram, Base64.DEFAULT))
                Pair(timestamp.toLong(), cryptogram)
            }
        }
    )
