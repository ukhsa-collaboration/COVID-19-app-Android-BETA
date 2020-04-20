/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import androidx.core.content.edit
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import javax.inject.Inject

class StateStorage @Inject constructor(context: Context) {

    private val storage by lazy {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    fun update(state: UserState) {
        storage.edit {
            putString(PREF_USER_STATE, UserStateSerialization.serialize(state))
        }
    }

    fun get(): UserState =
        storage.getString(PREF_USER_STATE, null)
            ?.let { UserStateSerialization.deserialize(it) }
            ?: DefaultState(DateTime.now(UTC))

    fun clear() {
        storage.edit { clear() }
    }

    companion object {
        const val PREFERENCE_NAME = "user_state_storage"
        const val PREF_USER_STATE = "user_state"
    }
}
