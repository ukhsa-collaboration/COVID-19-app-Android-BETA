/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationManager
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import javax.inject.Inject

class OkViewModel @Inject constructor(
    private val onboardingStatusProvider: OnboardingStatusProvider,
    private val sonarIdProvider: SonarIdProvider,
    private val registrationManager: RegistrationManager
) : ViewModel() {

    fun viewState(): LiveData<ViewState> {
        return Transformations.map(sonarIdProvider.hasProperSonarIdLiveData()) { hasProperSonarId ->
            if (hasProperSonarId) {
                ViewState.Success
            } else {
                ViewState.Progress
            }
        }
    }

    fun onStart() {
        onboardingStatusProvider.setOnboardingFinished(true)
        if (!sonarIdProvider.hasProperSonarId()) {
            registrationManager.register()
        }
    }
}
