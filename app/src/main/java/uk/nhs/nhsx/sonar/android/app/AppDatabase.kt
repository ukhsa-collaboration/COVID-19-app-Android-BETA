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
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactEventDao(): ContactEventDao
    abstract fun acknowledgementsDao(): AcknowledgementsDao
}

class Converters {

    @TypeConverter
    fun intListToString(value: List<Int>) = value.joinToString(separator = ",")

    @TypeConverter
    fun longListToString(value: List<Long>) = value.joinToString(separator = ",")

    @TypeConverter
    fun stringToIntList(value: String) = value.split(",").map { it.toInt() }

    @TypeConverter
    fun stringToLongList(value: String) = value.split(",").map { it.toLong() }
}
