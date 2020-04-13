package com.example.colocate.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesSonarIdProvider(context: Context) : SonarIdProvider {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("residentId", Context.MODE_PRIVATE)
    }

    override fun getSonarId(): String {
        return sharedPreferences.getString(KEY, ID_NOT_REGISTERED)!!
    }

    override fun hasProperSonarId(): Boolean {
        val sonarId = getSonarId()
        return sonarId.isNotEmpty() && sonarId != ID_NOT_REGISTERED
    }

    override fun setSonarId(sonarId: String) {
        sharedPreferences.edit { putString(KEY, sonarId) }
    }

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    companion object {
        private const val KEY = "RESIDENT_ID"
    }
}
