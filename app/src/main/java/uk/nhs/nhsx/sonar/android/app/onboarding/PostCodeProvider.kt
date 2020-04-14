package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context

class PostCodeProvider(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("postCode", Context.MODE_PRIVATE)
    }

    fun setPostCode(postCode: String) {
        sharedPreferences.edit().putString(KEY, postCode).apply()
    }

    fun getPostCode(): String {
        return sharedPreferences.getString(KEY, "") ?: ""
    }

    companion object {
        private const val KEY = "POST_CODE"
    }
}
