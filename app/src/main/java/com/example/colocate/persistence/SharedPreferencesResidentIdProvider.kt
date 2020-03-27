package com.example.colocate.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

const val ID_NOT_REGISTERED = "00000000-0000-0000-0000-000000000000"

class SharedPreferencesResidentIdProvider(context: Context) : ResidentIdProvider {

    companion object {
        private const val KEY = "RESIDENT_ID"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("residentId", Context.MODE_PRIVATE)
    }

    override fun getResidentId(): String {
        return sharedPreferences.getString(KEY, ID_NOT_REGISTERED)!!
    }

    override fun setResidentId(residentId: String) {
        sharedPreferences.edit { putString(KEY, residentId) }
    }
}
