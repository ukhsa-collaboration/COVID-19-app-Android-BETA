/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import android.content.Context
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import uk.nhs.nhsx.sonar.android.app.crypto.ELLIPTIC_CURVE
import uk.nhs.nhsx.sonar.android.app.crypto.PROVIDER_NAME
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

interface SecretKeyStorage {
    fun provideSecretKey(): SecretKey?
    fun storeSecretKey(encodedKey: String)
}

interface PublicKeyStorage {
    fun providePublicKey(): PublicKey?
    fun storeServerPublicKey(encodedKey: String)
}

interface KeyStorage : SecretKeyStorage, PublicKeyStorage

class DelegatingKeyStore @Inject constructor(
    private val secretKeyStorage: SecretKeyStorage,
    private val publicKeyStorage: PublicKeyStorage
) : KeyStorage {
    override fun provideSecretKey(): SecretKey? = secretKeyStorage.provideSecretKey()
    override fun storeSecretKey(encodedKey: String) = secretKeyStorage.storeSecretKey(encodedKey)
    override fun providePublicKey(): PublicKey? = publicKeyStorage.providePublicKey()
    override fun storeServerPublicKey(encodedKey: String) =
        publicKeyStorage.storeServerPublicKey(encodedKey)
}

const val PUBLIC_KEY_FILENAME = "server_public_key"
const val PREF_PUBLIC_KEY = "public_key"
const val SECRET_KEY_PREFERENCE_FILENAME = "key"
const val PREF_SECRET_KEY = "encryption_key"

class SharedPreferencesPublicKeyStorage(
    private val context: Context
) : PublicKeyStorage {
    override fun providePublicKey(): PublicKey? {
        val keyAsString = context
            .getSharedPreferences(
                PUBLIC_KEY_FILENAME,
                Context.MODE_PRIVATE
            )
            .getString(PREF_PUBLIC_KEY, null)
            ?: return null

        val decoded = Base64.decode(keyAsString, Base64.DEFAULT)
        val pubKeySpec = X509EncodedKeySpec(decoded)
        val ecKeyFactory = KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)
        return ecKeyFactory.generatePublic(pubKeySpec)
    }

    override fun storeServerPublicKey(encodedKey: String) {
        context.getSharedPreferences(PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_PUBLIC_KEY, encodedKey)
            .apply()
    }
}

private const val APP_SECRET_KEY_ALIAS = "secretKey"

class AndroidSecretKeyStorage(
    private val keyStore: KeyStore,
    private val context: Context
) : SecretKeyStorage {
    override fun provideSecretKey(): SecretKey? {
        val sharedPrefsKey = context
            .getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .getString(PREF_SECRET_KEY, null)
        val keyStoreKey = keyStore.getKey(APP_SECRET_KEY_ALIAS, null) as SecretKey?

        if (sharedPrefsKey == null) return keyStoreKey

        context.deleteSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME)
        if (keyStoreKey == null) {
            storeSecretKey(sharedPrefsKey)
            return keyStore.getKey(APP_SECRET_KEY_ALIAS, null) as SecretKey?
        }
        return keyStoreKey
    }

    override fun storeSecretKey(encodedKey: String) {
        val rawKey = Base64.decode(
            encodedKey,
            Base64.DEFAULT
        )
        keyStore.setEntry(
            APP_SECRET_KEY_ALIAS,
            KeyStore.SecretKeyEntry(
                SecretKeySpec(
                    rawKey,
                    KeyProperties.KEY_ALGORITHM_HMAC_SHA256
                )
            ),
            KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
                .setDigests(
                    KeyProperties.DIGEST_SHA256
                )
                .build()
        )
    }
}
