package com.example.colocate.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesResidentIdProvider(context: Context) : ResidentIdProvider {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("residentId", Context.MODE_PRIVATE)
    }

    override fun getResidentId(): String {
        return sharedPreferences.getString(KEY, ID_NOT_REGISTERED)!!
    }

    override fun hasProperResidentId(): Boolean {
        val residentId = getResidentId()
        return residentId.isNotEmpty() && residentId != ID_NOT_REGISTERED
    }

    override fun setResidentId(residentId: String) {
        sharedPreferences.edit { putString(KEY, residentId) }
    }

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    companion object {
        private const val KEY = "RESIDENT_ID"
    }
}
