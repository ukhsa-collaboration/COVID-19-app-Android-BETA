package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject

class OnboardingStatusProvider @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    }

    fun isOnboardingFinished(): Boolean =
        sharedPreferences.getBoolean(KEY, false)

    fun setOnboardingFinished(finished: Boolean) =
        sharedPreferences.edit { putBoolean(KEY, finished) }

    companion object {
        private const val KEY = "ONBOARDING_FINISHED"
    }
}
