package uk.nhs.nhsx.sonar.android.client.security

import android.content.Context
import android.util.Base64

class SharedPreferencesEncryptionKeyStorage(private val context: Context) : EncryptionKeyStorage {

    override fun provideKey(): ByteArray? {
        val keyAsString = context
            .getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY, null)
            ?: return null

        return Base64.decode(keyAsString, Base64.DEFAULT)
    }

    override fun putBase64Key(encodedKey: String) {
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
