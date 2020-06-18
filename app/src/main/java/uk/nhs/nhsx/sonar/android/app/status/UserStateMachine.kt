/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderScheduler
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider
import javax.inject.Inject

class UserStateMachine @Inject constructor(
    private val transitions: UserStateTransitions,
    private val userStateStorage: UserStateStorage,
    private val userInbox: UserInbox,
    private val reminderScheduler: ReminderScheduler
) {

    fun state(): UserState = userStateStorage.get()

    fun reset(): Unit = userStateStorage.set(DefaultState)

    fun diagnose(symptomsDate: LocalDate, symptoms: NonEmptySet<Symptom>) {
        val currentState = this.userStateStorage.get()

        val newState = transitions.diagnose(
            currentState,
            symptomsDate,
            symptoms
        )

        if (newState is DefaultState) {
            userInbox.addRecovery()
        }

        newState.scheduleReminder(reminderScheduler)
        this.userStateStorage.set(newState)
    }

    fun diagnoseCheckIn(symptoms: Set<Symptom>) {
        val currentState = this.userStateStorage.get()

        val newState = transitions.diagnoseForCheckin(
            currentState = currentState,
            symptoms = symptoms
        )
        if (newState is DefaultState && symptoms.isNotEmpty()) {
            userInbox.addRecovery()
        }
        this.userStateStorage.set(newState)
    }

    fun transitionOnExpiredExposedState() {
        val currentState = this.userStateStorage.get()

        val newState = transitions.transitionOnExpiredExposedState(currentState)
        this.userStateStorage.set(newState)
    }

    fun transitionOnExposure(exposureDate: DateTime, onStateChanged: () -> Unit = {}) {
        val currentState = this.userStateStorage.get()

        val newState = transitions.transitionOnExposure(
            currentState = currentState,
            exposureDate = exposureDate
        )

        if (newState != currentState) {
            this.userStateStorage.set(newState)
            newState.scheduleReminder(reminderScheduler)
            onStateChanged()
        }
    }

    fun transitionOnTestResult(testInfo: TestInfo) {
        val currentState = this.userStateStorage.get()

        val newState = transitions.transitionOnTestResult(currentState, testInfo)

        this.userStateStorage.set(newState)
        newState.scheduleReminder(reminderScheduler)
        userInbox.addTestInfo(testInfo)
    }

    fun hasAnyOfMainSymptoms(symptoms: Set<Symptom>): Boolean =
        symptoms.contains(Symptom.TEMPERATURE) ||
            symptoms.contains(Symptom.COUGH) ||
            symptoms.contains(Symptom.ANOSMIA)
}

class UserStateStorage @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<UserState>(
        context,
        preferenceName = "user_state_storage",
        preferenceKey = "user_state",
        serialize = UserStateSerialization::serialize,
        deserialize = UserStateSerialization::deserialize
    )
