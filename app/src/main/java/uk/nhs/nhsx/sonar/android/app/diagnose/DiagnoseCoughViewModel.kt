package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.RecoveryState
import uk.nhs.nhsx.sonar.android.app.status.RedState
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
                is DefaultState -> {
                    if (!hasCough and !hasTemperature) StateResult.Close
                    else StateResult.Review(hasCough)
                }
                is EmberState -> {
                    handleAmberState(hasTemperature, hasCough)
                }
                is RedState -> {
                    handleRedState(hasTemperature, hasCough)
                }
                is RecoveryState -> {
                    DefaultState(inOneDay()).run {
                        StateResult.Main(this)
                    }
                }
            }
        }
    }

    private fun handleRedState(
        hasTemperature: Boolean,
        hasCough: Boolean
    ): StateResult {
        return when {
            hasTemperature and hasCough -> {
                val newState = RedState(
                    inOneDay(),
                    setOf(Symptom.COUGH, Symptom.TEMPERATURE)
                )
                updateState(newState)
            }
            hasTemperature -> {
                val newState = RedState(
                    inOneDay(),
                    setOf(Symptom.TEMPERATURE)
                )
                updateState(newState)
            }
            hasCough -> {
                val newState = RecoveryState(
                    inOneDay()
                )
                updateState(newState)
            }
            else -> {
                val newState = DefaultState(
                    inOneDay()
                )
                updateState(newState)
            }
        }
    }

    private fun handleAmberState(
        hasTemperature: Boolean,
        hasCough: Boolean
    ): StateResult {
        return when {
            hasTemperature and hasCough -> {
                RedState(
                    inSevenDays(),
                    setOf(Symptom.COUGH, Symptom.TEMPERATURE)
                ).run {
                    updateState(this)
                }
            }
            hasTemperature -> {
                RedState(inSevenDays(), setOf(Symptom.TEMPERATURE)).run {
                    updateState(this)
                }
            }
            hasCough -> {
                RedState(inSevenDays(), setOf(Symptom.COUGH)).run {
                    updateState(this)
                }
            }
            else -> {
                StateResult.Main(prevState)
            }
        }
    }

    private fun updateState(newState: UserState): StateResult {
        stateStorage.update(newState)
        return StateResult.Main(newState)
    }

    private fun inSevenDays() = DateTime.now(UTC).plusDays(SEVEN_DAYS)

    private fun inOneDay() = DateTime.now(UTC).plusDays(ONE_DAY)

    companion object {
        private const val SEVEN_DAYS = 7
        private const val ONE_DAY = 1
    }
}

sealed class StateResult {

    data class Review(val hasCough: Boolean) : StateResult()

    object Close : StateResult()

    data class Main(val userState: UserState) : StateResult()
}
