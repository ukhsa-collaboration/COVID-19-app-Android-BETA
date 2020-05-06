/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Close
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Main
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Review
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateFactory
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
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

            val symptoms = symptoms(hasTemperature, hasCough)

            nextStateLiveData.value = when (prevState.displayState()) {
                ISOLATE -> Main(updateState(symptoms))
                else -> {
                    if (symptoms.isEmpty()) Close else Review(NonEmptySet.create(symptoms)!!)
                }
            }
        }
    }

    private fun updateState(symptoms: Set<Symptom>): UserState =
        UserStateFactory.checkinQuestionnaire(symptoms).also {
            userStateStorage.update(it)
        }

    private fun symptoms(hasTemperature: Boolean, hasCough: Boolean): Set<Symptom> =
        listOfNotNull(
            if (hasTemperature) TEMPERATURE else null,
            if (hasCough) COUGH else null
        ).toSet()
}

sealed class StateResult {

    data class Review(val symptoms: NonEmptySet<Symptom>) : StateResult()

    object Close : StateResult()

    data class Main(val userState: UserState) : StateResult()
}
