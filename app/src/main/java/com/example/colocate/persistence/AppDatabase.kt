/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [ContactEvent::class, ContactEventV2::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactEventDao(): ContactEventDao
    abstract fun contactEventV2Dao(): ContactEventV2Dao
}

class Converters {

    @TypeConverter
    fun listToString(value: List<Int>) = value.joinToString(separator = ",")

    @TypeConverter
    fun stringToList(value: String) = value.split(",").map { it.toInt() }
}
