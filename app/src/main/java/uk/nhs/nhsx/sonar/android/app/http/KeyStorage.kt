/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import android.content.Context
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import androidx.core.content.edit
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

class SharedPreferencesPublicKeyStorage(context: Context) : PublicKeyStorage {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE)
    }

    override fun providePublicKey(): PublicKey? =
        sharedPreferences
            .getString(PREF_PUBLIC_KEY, null)
            ?.let { keyAsString ->
                val decoded = keyAsString.base64Decode()
                val pubKeySpec = X509EncodedKeySpec(decoded)
                val ecKeyFactory = KeyFactory.getInstance(ELLIPTIC_CURVE, PROVIDER_NAME)

                ecKeyFactory.generatePublic(pubKeySpec)
            }

    override fun storeServerPublicKey(encodedKey: String) =
        sharedPreferences
            .edit { putString(PREF_PUBLIC_KEY, encodedKey) }
}

private const val APP_SECRET_KEY_ALIAS = "secretKey"

class AndroidSecretKeyStorage(
    private val keyStore: KeyStore,
    private val context: Context
) : SecretKeyStorage {

    override fun provideSecretKey(): SecretKey? {
        val sharedPrefsKey = getFromSharedPreferences()
        val keyStoreKey = getFromKeyStore()
        val hasAlreadyBeenMigrated = (sharedPrefsKey == null)

        if (hasAlreadyBeenMigrated) {
            return keyStoreKey
        }

        return migrateSharedPreferencesToKeystore(keyStoreKey, sharedPrefsKey!!)
    }

    override fun storeSecretKey(encodedKey: String) {
        val rawKey = encodedKey.base64Decode()
        val secretKeyEntry = KeyStore.SecretKeyEntry(
            SecretKeySpec(rawKey, KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
        )
        keyStore.setEntry(APP_SECRET_KEY_ALIAS, secretKeyEntry, protectionParameters)
    }

    private val protectionParameters = KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
        .setDigests(KeyProperties.DIGEST_SHA256)
        .build()

    private fun migrateSharedPreferencesToKeystore(keyStoreKey: SecretKey?, sharedPrefsKey: String): SecretKey? {
        context.deleteSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME)

        if (keyStoreKey == null) {
            storeSecretKey(sharedPrefsKey)
            return getFromKeyStore()
        }
        return keyStoreKey
    }

    private fun getFromSharedPreferences(): String? =
        context
            .getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .getString(PREF_SECRET_KEY, null)

    private fun getFromKeyStore(): SecretKey? =
        keyStore.getKey(APP_SECRET_KEY_ALIAS, null) as SecretKey?
}

private fun String.base64Decode() = Base64.decode(this, Base64.DEFAULT)
