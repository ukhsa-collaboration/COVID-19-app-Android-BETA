/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.status

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

class SharedPreferencesStatusStorage(private val context: Context) : StatusStorage {
    override fun update(status: CovidStatus) {
        getPreferences()
            .edit()
            .putString(PREF_STATUS, status.name)
            .apply()
    }

    override fun get(): CovidStatus =
        getPreferences()
            .getString(PREF_STATUS, "OK")
            ?.let { CovidStatus.valueOf(it) }
            ?: CovidStatus.OK

    // Used in tests, not available on the interface
    fun reset() {
        getPreferences()
            .edit()
            .clear()
            .apply()
    }

    private fun getPreferences(): SharedPreferences =
        context.getSharedPreferences(PREFERENCE_FILENAME, MODE_PRIVATE)

    companion object {
        const val PREFERENCE_FILENAME = "status"
        const val PREF_STATUS = "user_status"
    }
}
