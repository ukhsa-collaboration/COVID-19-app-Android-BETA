/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import androidx.room.Room
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.KeyProvider
import com.example.colocate.persistence.ResidentIdProvider
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
        return object : ResidentIdProvider {
            override fun getResidentId(): String {
                return "80baf81b-8afd-47e9-9915-50691525c910"
            }
        }
    }


    @Provides
    fun provideKeyProvider(): KeyProvider {
        return object : KeyProvider {
            override fun getKey(): ByteArray {
                return "JXRezte6OJ8MUavY28hsia6XiF92geOf82TKB5Qp+QQ=".toByteArray()
            }

        }
    }
}