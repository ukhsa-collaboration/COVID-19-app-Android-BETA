package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.RecoveryState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateFactory
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserState
import javax.inject.Inject

class DiagnoseCoughViewModel @Inject constructor(private val stateStorage: StateStorage) :
    ViewModel() {

    private val prevState: UserState by lazy {
        stateStorage.get()
    }

    private val nextStateLiveData = MutableLiveData<StateResult>()

    fun observeUserState(): LiveData<StateResult> = nextStateLiveData

    fun update(hasTemperature: Boolean, hasCough: Boolean) {
        viewModelScope.launch {
            nextStateLiveData.value = when (prevState) {
                is RedState -> handleSimplified(hasTemperature, hasCough)
                else -> handleNormal(hasTemperature, hasCough)
            }
        }
    }

    private fun handleSimplified(hasTemperature: Boolean, hasCough: Boolean): StateResult {
        return when {
            hasTemperature and hasCough ->
                StateFactory.extendedRed(Symptom.COUGH, Symptom.TEMPERATURE)

            hasTemperature ->
                StateFactory.extendedRed(Symptom.TEMPERATURE)

            hasCough ->
                RecoveryState(inOneDay())

            else ->
                DefaultState(inOneDay())
        }.let {
            updateState(it)
        }
    }

    private fun handleNormal(hasTemperature: Boolean, hasCough: Boolean): StateResult =
        if (!hasCough and !hasTemperature) StateResult.Close else StateResult.Review(hasCough)

    private fun updateState(newState: UserState): StateResult {
        stateStorage.update(newState)
        return StateResult.Main(newState)
    }

    private fun inOneDay() = DateTime.now(UTC).plusDays(1)
}

sealed class StateResult {

    data class Review(val hasCough: Boolean) : StateResult()

    object Close : StateResult()

    data class Main(val userState: UserState) : StateResult()
}
