/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
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

    fun set(state: UserState): Unit = userStatePrefs.set(state)

    fun clear(): Unit = userStatePrefs.clear()

    fun diagnose(symptomsDate: LocalDate, symptoms: NonEmptySet<Symptom>): UserState {
        val currentState = get()
        val newState = UserStateTransitions.diagnose(
            currentState,
            symptomsDate,
            NonEmptySet.create(symptoms)!!
        )

        if (newState is DefaultState && symptoms.isNotEmpty()) {
            userInbox.addRecovery()
        }

        newState.scheduleCheckInReminder(reminders)
        set(newState)

        Timber.d("Updated the state to: $newState")
        return newState
    }

    fun diagnoseCheckIn(symptoms: Set<Symptom>): UserState {
        val currentState = get()
        val newState = UserStateTransitions.diagnoseForCheckin(
            currentState = currentState,
            symptoms = symptoms
        )
        if (newState is DefaultState && symptoms.isNotEmpty()) {
            userInbox.addRecovery()
        }
        set(newState)

        Timber.d("Updated the state to: $newState")

        return newState
    }
}

class UserStatePrefs @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<UserState>(
        context,
        preferenceName = "user_state_storage",
        preferenceKey = "user_state",
        serialize = UserStateSerialization::serialize,
        deserialize = UserStateSerialization::deserialize
    )
