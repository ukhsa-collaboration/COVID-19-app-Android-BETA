/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import android.content.Context
import androidx.core.content.edit
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceProvider
import javax.inject.Inject

class OnboardingStatusProvider @Inject constructor(context: Context) :
    SharedPreferenceProvider<Boolean>(
        context,
        preferenceName = "onboarding",
        preferenceKey = "ONBOARDING_FINISHED"
    ) {

    override fun get(): Boolean =
        sharedPreferences.getBoolean(preferenceKey, false)

    override fun set(value: Boolean) =
        sharedPreferences.edit { putBoolean(preferenceKey, value) }
}
