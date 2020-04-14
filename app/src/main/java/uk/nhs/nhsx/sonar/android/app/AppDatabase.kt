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
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventV2
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventV2Dao
import uk.nhs.nhsx.sonar.android.app.notifications.Acknowledgement
import uk.nhs.nhsx.sonar.android.app.notifications.AcknowledgementsDao

@Database(
    entities = [ContactEvent::class, ContactEventV2::class, Acknowledgement::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactEventDao(): ContactEventDao
    abstract fun contactEventV2Dao(): ContactEventV2Dao
    abstract fun acknowledgementsDao(): AcknowledgementsDao
}

class Converters {

    @TypeConverter
    fun listToString(value: List<Int>) = value.joinToString(separator = ",")

    @TypeConverter
    fun stringToList(value: String) = value.split(",").map { it.toInt() }
}
