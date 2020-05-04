package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Close
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Review
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.RecoveryState
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateFactory
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf
import javax.inject.Inject

class DiagnoseCoughViewModel @Inject constructor(private val userStateStorage: UserStateStorage) :
    ViewModel() {

    private val prevState: UserState by lazy {
        userStateStorage.get()
    }

    private val nextStateLiveData = MutableLiveData<StateResult>()

    fun observeUserState(): LiveData<StateResult> = nextStateLiveData

    fun update(hasTemperature: Boolean, hasCough: Boolean) {
        viewModelScope.launch {
            nextStateLiveData.value = when (prevState.displayState()) {
                ISOLATE -> handleSimplified(hasTemperature, hasCough)
                else -> handleNormal(hasTemperature, hasCough)
            }
        }
    }

    private fun handleSimplified(hasTemperature: Boolean, hasCough: Boolean): StateResult {
        val userState = when {
            hasTemperature and hasCough -> UserStateFactory.checkin(nonEmptySetOf(COUGH, TEMPERATURE))
            hasTemperature -> UserStateFactory.checkin(nonEmptySetOf(TEMPERATURE))
            hasCough -> RecoveryState
            else -> DefaultState
        }
        return updateState(userState)
    }

    private fun handleNormal(hasTemperature: Boolean, hasCough: Boolean): StateResult =
        when {
            hasTemperature and hasCough -> Review(nonEmptySetOf(TEMPERATURE, COUGH))
            hasTemperature -> Review(nonEmptySetOf(TEMPERATURE))
            hasCough -> Review(nonEmptySetOf(COUGH))
            else -> Close
        }

    private fun updateState(newState: UserState): StateResult {
        userStateStorage.update(newState)
        return StateResult.Main(newState)
    }
}

sealed class StateResult {

    data class Review(val symptoms: NonEmptySet<Symptom>) : StateResult()

    object Close : StateResult()

    data class Main(val userState: UserState) : StateResult()
}
