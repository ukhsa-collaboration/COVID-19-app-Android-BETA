/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject

const val ID_NOT_REGISTERED = "00000000-0000-0000-0000-000000000000"

class SonarIdProvider @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("residentId", Context.MODE_PRIVATE)
    }

    fun getSonarId(): String {
        return sharedPreferences.getString(
            KEY,
            ID_NOT_REGISTERED
        )!!
    }

    fun hasProperSonarId(): Boolean {
        val sonarId = getSonarId()
        return sonarId.isNotEmpty() && sonarId != ID_NOT_REGISTERED
    }

    fun setSonarId(sonarId: String) {
        sharedPreferences.edit { putString(KEY, sonarId) }
    }

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    companion object {
        private const val KEY = "RESIDENT_ID"
    }
}
