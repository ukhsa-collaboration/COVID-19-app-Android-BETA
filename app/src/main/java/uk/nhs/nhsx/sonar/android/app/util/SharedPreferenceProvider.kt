package uk.nhs.nhsx.sonar.android.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

abstract class SharedPreferenceProvider<T>(
    context: Context,
    preferenceName: String,
    protected val preferenceKey: String
) {

    protected val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    }

    fun clear() = sharedPreferences.edit { clear() }

    fun has(): Boolean = sharedPreferences.contains(preferenceKey)

    abstract fun get(): T

    abstract fun set(value: T)
}

open class SharedPreferenceStringProvider(
    context: Context,
    preferenceName: String,
    preferenceKey: String,
    private val defaultValue: String = ""
) : SharedPreferenceProvider<String>(context, preferenceName, preferenceKey) {

    override fun get(): String =
        sharedPreferences.getString(preferenceKey, defaultValue) ?: defaultValue

    override fun set(value: String) =
        sharedPreferences.edit { putString(preferenceKey, value) }
}

open class SharedPreferenceSerializingProvider<T>(
    context: Context,
    preferenceName: String,
    preferenceKey: String,
    private val serialize: (T) -> String?,
    private val deserialize: (String?) -> T
) : SharedPreferenceProvider<T>(context, preferenceName, preferenceKey) {

    override fun get(): T =
        sharedPreferences.getString(preferenceKey, null).let(deserialize)

    override fun set(value: T) =
        sharedPreferences.edit { putString(preferenceKey, value?.let(serialize)) }
}
