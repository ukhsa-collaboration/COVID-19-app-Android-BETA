/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.registration

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject

class SonarIdProvider @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("residentId", Context.MODE_PRIVATE)
    }

    fun getSonarId(): String =
        sharedPreferences.getString(KEY, "")!!

    fun hasProperSonarId(): Boolean =
        getSonarId().isNotEmpty()

    fun setSonarId(sonarId: String) =
        sharedPreferences.edit { putString(KEY, sonarId) }

    fun clear() =
        sharedPreferences.edit { clear() }

    companion object {
        private const val KEY = "RESIDENT_ID"
    }
}
