/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.referencecode

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceCodeProvider @Inject constructor(context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("referenceCode", Context.MODE_PRIVATE)
    }

    fun get(): ReferenceCode? =
        sharedPreferences.getString(KEY, null)?.let { ReferenceCode(it) }

    fun set(code: ReferenceCode) =
        sharedPreferences.edit { putString(KEY, code.value) }

    fun clear() =
        sharedPreferences.edit { clear() }

    companion object {
        private const val KEY = "REFERENCE_CODE"
    }
}
