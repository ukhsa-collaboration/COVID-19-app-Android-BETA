/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEvent
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.notifications.Acknowledgement
import uk.nhs.nhsx.sonar.android.app.notifications.AcknowledgementsDao

@Database(
    entities = [ContactEvent::class, Acknowledgement::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactEventV2Dao(): ContactEventDao
    abstract fun acknowledgementsDao(): AcknowledgementsDao
}

class Converters {

    @TypeConverter
    fun listToString(value: List<Int>) = value.joinToString(separator = ",")

    @TypeConverter
    fun stringToList(value: String) = value.split(",").map { it.toInt() }
}
