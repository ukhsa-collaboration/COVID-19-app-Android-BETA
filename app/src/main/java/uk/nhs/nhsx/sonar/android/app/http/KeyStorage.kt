/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import android.content.Context
import android.util.Base64
import android.util.Log
import uk.nhs.nhsx.sonar.android.app.crypto.ELLIPTIC_CURVE
import uk.nhs.nhsx.sonar.android.app.crypto.PROVIDER_NAME
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

interface KeyStorage {
    fun provideSecretKey(): ByteArray?
    fun storeSecretKey(encodedKey: String)
    fun providePublicKey(): PublicKey?
    fun storeServerPublicKey(encodedKey: String)
}

class SharedPreferencesKeyStorage @Inject constructor(
    private val context: Context
) : KeyStorage {

    override fun provideSecretKey(): ByteArray? {
        val keyAsString = context
            .getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .getString(PREF_SECRET_KEY, null)
            ?: return null

        return Base64.decode(keyAsString, Base64.DEFAULT)
    }

    override fun storeSecretKey(encodedKey: String) {
        context.getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_SECRET_KEY, encodedKey)
            .apply()
    }

    override fun providePublicKey(): PublicKey? {
        val keyAsString = context
            .getSharedPreferences(
                PUBLIC_KEY_FILENAME,
                Context.MODE_PRIVATE
            )
            .getString(PREF_PUBLIC_KEY, null)
            ?: return null

        val ecKeyFactory = KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
        val decoded = Base64.decode(keyAsString, Base64.DEFAULT)
        val pubKeySpec = X509EncodedKeySpec(decoded)
        return ecKeyFactory.generatePublic(pubKeySpec)
    }

    override fun storeServerPublicKey(encodedKey: String) {
        Log.d("KEY PROV", "storing encoded $encodedKey")

        context.getSharedPreferences(PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_PUBLIC_KEY, encodedKey)
            .apply()
    }

    companion object {
        const val PUBLIC_KEY_FILENAME = "server_public_key"
        const val PREF_PUBLIC_KEY = "public_key"
        const val SECRET_KEY_PREFERENCE_FILENAME = "key"
        const val PREF_SECRET_KEY = "encryption_key"
    }
}
