/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import androidx.room.Room
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ResidentIdProvider
import com.example.colocate.persistence.SharedPreferencesResidentIdProvider
import dagger.Module
import dagger.Provides

@Module
class PersistenceModule(private val applicationContext: Context) {
    @Provides
    fun provideDatabase() =
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "event-database"
        ).build()

    @Provides
    fun provideContactEventDao(database: AppDatabase): ContactEventDao {
        return database.contactEventDao()
    }

    @Provides
    fun provideResidentIdProvider(): ResidentIdProvider {
        return SharedPreferencesResidentIdProvider(applicationContext)
    }
}
