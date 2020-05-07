/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Close
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Main
import uk.nhs.nhsx.sonar.android.app.diagnose.StateResult.Review
import uk.nhs.nhsx.sonar.android.app.status.DisplayState.ISOLATE
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.status.UserState
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import uk.nhs.nhsx.sonar.android.app.status.UserStateTransitions
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnoseCoughForm @Inject constructor(private val userStateStorage: UserStateStorage) {

    fun submit(hasTemperature: Boolean, hasCough: Boolean): StateResult {
        val prevState = userStateStorage.get()
        val symptoms = symptoms(hasTemperature, hasCough)
        val isCurrentlyIsolating = prevState.displayState() == ISOLATE
        val hasNoSymptoms = symptoms.isEmpty()

        return when {
            isCurrentlyIsolating -> Main(updateState(symptoms))
            hasNoSymptoms -> Close
            else -> Review(NonEmptySet.create(symptoms)!!)
        }
    }

    private fun updateState(symptoms: Set<Symptom>): UserState =
        UserStateTransitions.diagnoseForCheckin(symptoms).also {
            userStateStorage.set(it)
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
