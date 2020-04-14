package uk.nhs.nhsx.sonar.android.app.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesOnboardingStatusProvider(context: Context) : OnboardingStatusProvider {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    }

    override fun isOnboardingFinished(): Boolean {
        return sharedPreferences.getBoolean(KEY, false)
    }

    override fun setOnboardingFinished(finished: Boolean) {
        sharedPreferences.edit { putBoolean(KEY, finished) }
    }

    companion object {
        private const val KEY = "ONBOARDING_FINISHED"
    }
}
