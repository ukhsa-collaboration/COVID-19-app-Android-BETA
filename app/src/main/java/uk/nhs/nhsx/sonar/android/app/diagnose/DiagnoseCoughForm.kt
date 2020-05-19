/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Close
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Main
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Review
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnoseCoughForm @Inject constructor(private val userStateStorage: UserStateStorage) {

    fun submit(symptoms: Set<Symptom>): StateResult {
        val prevState = userStateStorage.get()
        val isCurrentlyIsolating = prevState.displayState() == ISOLATE
        val hasNoSymptoms = symptoms.isEmpty()

        return when {
            isCurrentlyIsolating -> Main(updateState(symptoms))
            hasNoSymptoms -> Close
            else -> Review
        }
    }

    private fun updateState(symptoms: Set<Symptom>): UserState =
        UserStateTransitions.diagnoseForCheckin(symptoms).also {
            userStateStorage.set(it)
        }
}

sealed class StateResult {
    object Review : StateResult()
    object Close : StateResult()
    data class Main(val userState: UserState) : StateResult()
}
