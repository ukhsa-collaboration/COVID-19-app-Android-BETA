package uk.nhs.nhsx.sonar.android.app.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.nhs.nhsx.sonar.android.app.ViewState
import uk.nhs.nhsx.sonar.android.app.persistence.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationResult
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationUseCase
import java.util.Date
import javax.inject.Inject

class OkViewModel @Inject constructor(
    private val registrationUseCase: RegistrationUseCase,
    private val onboardingStatusProvider: OnboardingStatusProvider,
    private val sonarIdProvider: SonarIdProvider
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> {
        return viewState
    }

    fun register() {
        viewModelScope.launch {
            viewState.value = ViewState.Progress
            val startTime = Date().time
            val registrationResult = registrationUseCase.register()
            viewState.value = when (registrationResult) {
                RegistrationResult.Success, RegistrationResult.AlreadyRegistered -> ViewState.Success
                is RegistrationResult.Failure -> {
                    waitForReadability(startTime)
                    ViewState.Error
                }
            }
        }
    }

    private suspend fun waitForReadability(startTime: Long) {
        val elapsedTime = Date().time - startTime
        val remainingDelay = MINIMUM_DELAY_FOR_READING - elapsedTime
        delay(remainingDelay)
    }

    fun onStart() {
        if (sonarIdProvider.hasProperSonarId()) {
            viewState.value = ViewState.Success
        } else {
            val userSeesThisScreenForTheFirstTime = !onboardingStatusProvider.isOnboardingFinished()
            if (userSeesThisScreenForTheFirstTime) {
                onboardingStatusProvider.setOnboardingFinished(true)
                register()
            } else {
                // registration failed previously. let the user decide when to retry registration
                viewState.value = ViewState.Error
            }
        }
    }

    companion object {
        const val MINIMUM_DELAY_FOR_READING = 2_000
    }
}
