package com.example.colocate.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


interface FCMPushTokenProvider {

    fun setToken(token: String)

    fun getToken(): String
}

class FCMPushTokenPreferences(context: Context) : FCMPushTokenProvider {

    companion object {
        private const val KEY = "TOKEN"
    }

    val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("fcm", Context.MODE_PRIVATE)
    }


    override fun setToken(token: String) {
        sharedPreferences.edit { putString(KEY, token) }
    }

    override fun getToken(): String {
        return sharedPreferences.getString(KEY, "")!!
    }


}

