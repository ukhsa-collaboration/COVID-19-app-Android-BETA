/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.status.SharedPreferencesStatusStorage
import uk.nhs.nhsx.sonar.android.app.status.StatusStorage

@Module
class StatusModule(private val context: Context) {
    @Provides
    fun providesStatusStorage(): StatusStorage {
        return SharedPreferencesStatusStorage(context)
    }
}
