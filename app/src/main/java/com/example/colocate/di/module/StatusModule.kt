/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import com.example.colocate.status.SharedPreferencesStatusStorage
import com.example.colocate.status.StatusStorage
import dagger.Module
import dagger.Provides

@Module
class StatusModule(private val context: Context) {
    @Provides
    fun providesStatusStorage(): StatusStorage {
        return SharedPreferencesStatusStorage(context)
    }
}
