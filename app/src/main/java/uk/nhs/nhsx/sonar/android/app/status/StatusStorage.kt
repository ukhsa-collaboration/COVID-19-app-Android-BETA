/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject

class StatusStorage @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("status", Context.MODE_PRIVATE)
    }

    fun update(status: CovidStatus) {
        sharedPreferences.edit { putString(PREF_STATUS, status.name) }
    }

    fun get(): CovidStatus =
        sharedPreferences
            .getString(PREF_STATUS, "OK")
            ?.let { CovidStatus.valueOf(it) }
            ?: CovidStatus.OK

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    companion object {
        const val PREF_STATUS = "user_status"
    }
}

enum class CovidStatus {
    OK,
    POTENTIAL,
    RED
}
