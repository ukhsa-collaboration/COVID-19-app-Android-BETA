/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.status

import android.content.Context

class SharedPreferencesStatusStorage(private val context: Context) : StatusStorage {
    override fun update(status: CovidStatus) {
        context.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_STATUS, status.name)
            .apply()
    }

    override fun get(): CovidStatus {
        val status = context.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .getString(PREF_STATUS, "OK")
            ?: return CovidStatus.OK
        return CovidStatus.valueOf(status)
    }

    companion object {
        const val PREFERENCE_FILENAME = "status"
        const val PREF_STATUS = "user_status"
    }
}
