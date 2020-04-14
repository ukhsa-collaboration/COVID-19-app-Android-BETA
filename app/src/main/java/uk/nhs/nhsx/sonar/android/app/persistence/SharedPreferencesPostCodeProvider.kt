package uk.nhs.nhsx.sonar.android.app.persistence

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesPostCodeProvider(context: Context) : PostCodeProvider {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("postCode", Context.MODE_PRIVATE)
    }

    override fun setPostCode(postCode: String) {
        sharedPreferences.edit().putString(KEY, postCode).apply()
    }

    override fun getPostCode(): String {
        return sharedPreferences.getString(KEY, "") ?: ""
    }

    companion object {
        private const val KEY = "POST_CODE"
    }
}
