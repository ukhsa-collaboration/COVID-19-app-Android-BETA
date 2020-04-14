package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import javax.inject.Inject

class PostCodeProvider @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("postCode", Context.MODE_PRIVATE)
    }

    fun setPostCode(postCode: String) =
        sharedPreferences.edit().putString(KEY, postCode).apply()

    fun getPostCode(): String =
        sharedPreferences.getString(KEY, "") ?: ""

    companion object {
        private const val KEY = "POST_CODE"
    }
}
