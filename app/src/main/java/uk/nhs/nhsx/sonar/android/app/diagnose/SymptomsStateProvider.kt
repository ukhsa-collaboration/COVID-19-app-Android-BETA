/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import androidx.core.content.edit
import com.google.gson.GsonBuilder
import uk.nhs.nhsx.sonar.android.app.status.CovidStatus
import javax.inject.Inject

class SymptomsStateProvider @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    fun update(state: SymptomsState) {
        GsonBuilder().create().toJson(state).apply {
            sharedPreferences.edit {
                putString(STATE_KEY, this@apply)
            }
        }
    }

    fun getOrDefault(): SymptomsState =
        sharedPreferences.getString(STATE_KEY, "")?.let {
            GsonBuilder().create().fromJson(it, SymptomsState::class.java)
        } ?: SymptomsState()

    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }

    companion object {
        const val PREFERENCE_NAME = "state_pref"
        const val STATE_KEY = "state"
    }
}

data class SymptomsState(
    var status: CovidStatus = CovidStatus.OK,
    var dateStarted: Long? = null,
    var hasCough: Boolean = false,
    var hasTemperature: Boolean = false
)
