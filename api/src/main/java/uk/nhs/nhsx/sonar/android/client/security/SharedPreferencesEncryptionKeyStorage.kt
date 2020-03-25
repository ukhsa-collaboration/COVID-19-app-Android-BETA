package uk.nhs.nhsx.sonar.android.client.security

import android.content.Context
import android.util.Base64

class SharedPreferencesEncryptionKeyStorage(private val context: Context) :
    EncryptionKeyStorage {
    override fun provideKey(): ByteArray? {
        val keyAsString = context.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY, "JXRezte6OJ8MUavY28hsia6XiF92geOf82TKB5Qp+QQ=") ?: return null
        return Base64.decode(keyAsString, Base64.DEFAULT)
    }

    override fun putKey(key: ByteArray) {
        val encodedKey = Base64.encodeToString(key, Base64.NO_WRAP)
        context.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY, encodedKey)
            .apply()
    }

    companion object {
        const val PREFERENCE_FILENAME = "key"
        const val PREF_KEY = "encryption_key"
    }
}