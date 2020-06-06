/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.LocalDate
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.util.NonEmptySet
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider
import javax.inject.Inject

class UserStateStorage @Inject constructor(
    private val userStatePrefs: UserStatePrefs,
    private val userInbox: UserInbox,
    private val reminders: Reminders
) {

    fun get(): UserState = userStatePrefs.get()

    fun set(state: UserState) {
        userStatePrefs.set(state)
        Timber.d("Updated the state to: $state")
    }

    fun clear(): Unit = userStatePrefs.clear()

    fun diagnose(symptomsDate: LocalDate, symptoms: NonEmptySet<Symptom>) {
        val currentState = this.userStatePrefs.get()

        val newState = UserStateTransitions.diagnose(
            currentState,
            symptomsDate,
            NonEmptySet.create(symptoms)!!
        )

        if (newState is DefaultState) {
            userInbox.addRecovery()
        }

        newState.scheduleCheckInReminder(reminders)
        this.userStatePrefs.set(newState)
    }

    fun diagnoseCheckIn(symptoms: Set<Symptom>) {
        val currentState = this.userStatePrefs.get()

        val newState = UserStateTransitions.diagnoseForCheckin(
            currentState = currentState,
            symptoms = symptoms
        )
        if (newState is DefaultState && symptoms.isNotEmpty()) {
            userInbox.addRecovery()
        }
        this.userStatePrefs.set(newState)
    }

    fun transitionOnExpiredExposedState() {
        val currentState = this.userStatePrefs.get()

        val newState = UserStateTransitions.transitionOnExpiredExposedState(currentState)
        this.userStatePrefs.set(newState)
    }

    fun transitionOnContactAlert(date: DateTime, onStateChanged: () -> Unit) {
        val currentState = this.userStatePrefs.get()

        val newState = UserStateTransitions.transitionOnContactAlert(
            currentState = currentState,
            exposureDate = date
        )

        if (newState != currentState) {
            this.userStatePrefs.set(newState)
            onStateChanged()
        }
    }

    fun hasAnyOfMainSymptoms(symptoms: Set<Symptom>): Boolean =
        UserStateTransitions.hasAnyOfMainSymptoms(symptoms)
}

class UserStatePrefs @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<UserState>(
        context,
        preferenceName = "user_state_storage",
        preferenceKey = "user_state",
        serialize = UserStateSerialization::serialize,
        deserialize = UserStateSerialization::deserialize
    )
