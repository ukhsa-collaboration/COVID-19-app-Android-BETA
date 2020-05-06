package uk.nhs.nhsx.sonar.android.app.status

import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.status.UserState.Companion.NO_DAYS_IN_RED
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.isMoreThan

object UserStateTransitions {

    fun diagnose(
        symptomsDate: LocalDate,
        symptoms: NonEmptySet<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState =
        if (doesNotHaveTemperature(symptoms) && symptomsDate.isMoreThan(days = NO_DAYS_IN_RED, from = today)) {
            RecoveryState
        } else {
            UserState.red(
                symptomsDate,
                symptoms,
                today
            )
        }

    fun diagnoseForCheckin(
        symptoms: Set<Symptom>,
        today: LocalDate = LocalDate.now()
    ): UserState =
        when {
            hasTemperature(symptoms) -> UserState.checkin(
                NonEmptySet.create(symptoms)!!,
                today
            )
            hasCough(symptoms) -> RecoveryState
            else -> DefaultState
        }

    fun transitionOnContactAlert(currentState: UserState): UserState? =
        when (currentState.displayState()) {
            DisplayState.OK -> UserState.amber()
            else -> null
        }

    fun transitionIfExpired(currentState: UserState): UserState? =
        if (currentState.hasExpired()) DefaultState else null

    private fun doesNotHaveTemperature(symptoms: Set<Symptom>): Boolean =
        !hasTemperature(symptoms)

    private fun hasTemperature(symptoms: Set<Symptom>): Boolean =
        Symptom.TEMPERATURE in symptoms

    private fun hasCough(symptoms: Set<Symptom>): Boolean =
        Symptom.COUGH in symptoms
}
