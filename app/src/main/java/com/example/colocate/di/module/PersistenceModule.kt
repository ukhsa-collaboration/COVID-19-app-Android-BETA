/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import androidx.room.Room
import com.example.colocate.persistence.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

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
        return object : ResidentIdProvider {
            override fun getResidentId(): String {
                return "80baf81b-8afd-47e9-9915-50691525c910"
            }

            override fun setResidentId(residentId: String) {

            }
        }
    }

    @Singleton
    @Provides
    fun provideFCMTokenProvider(): FCMPushTokenProvider =
        FCMPushTokenPreferences(applicationContext)

}
