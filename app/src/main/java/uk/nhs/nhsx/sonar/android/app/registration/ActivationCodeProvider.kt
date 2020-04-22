package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject

class ActivationCodeProvider @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("activationCode", Context.MODE_PRIVATE)
    }

    fun setActivationCode(activationCode: String) =
        sharedPreferences.edit().putString(KEY, activationCode).apply()

    fun getActivationCode(): String =
        sharedPreferences.getString(KEY, "") ?: ""

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    companion object {
        private const val KEY = "ACTIVATION_CODE"
    }
}
