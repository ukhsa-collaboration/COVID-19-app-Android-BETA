/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Seconds
import timber.log.Timber
import kotlin.math.abs

@Dao
interface ContactEventDao {
    @Insert
    fun insert(contactEvent: ContactEvent)

    @Update
    fun update(contactEvent: ContactEvent)

    @Transaction
    suspend fun createOrUpdate(newEvent: ContactEvent, errorMargin: Int) {
        val sorted = getAll().sortedBy { it.timestamp }
        val eventsById = sorted.filter { it.sonarId.contentEquals(newEvent.sonarId) }
        for (event in eventsById.subList(0, eventsById.size)) {
            val newEventTime = DateTime(newEvent.timestamp, DateTimeZone.UTC)
            val storedEventTime = DateTime(event.timestamp, DateTimeZone.UTC)
            val diff = abs(Seconds.secondsBetween(newEventTime, storedEventTime).seconds)

            // TODO: What happens if the new event is in the middle of the existing one?
            // Need to capture timestamp for each reading?
            if (Seconds.seconds(diff).plus(event.duration).isLessThan(Seconds.seconds(errorMargin))
            ) {
                val rssis = event.rssiValues

                val updatedEvent = if (newEventTime.isAfter(storedEventTime)) {
                    event.copy(
                        duration = event.duration + diff,
                        rssiValues = rssis.plus(newEvent.rssiValues)
                    )
                } else {
                    event.copy(
                        timestamp = newEvent.timestamp,
                        duration = event.duration + diff,
                        rssiValues = newEvent.rssiValues.plus(rssis)
                    )
                }
                Timber.d("saving Updating event $updatedEvent")
                update(updatedEvent)
                return
            }
        }
        // no event within error margin
        Timber.d("saving No matching event; inserting $newEvent")
        insert(newEvent)
    }

    @Query("SELECT * FROM ${ContactEvent.TABLE_NAME}")
    suspend fun getAll(): List<ContactEvent>

    @Query("DELETE FROM ${ContactEvent.TABLE_NAME}")
    suspend fun clearEvents()

    @Query("DELETE FROM ${ContactEvent.TABLE_NAME} WHERE timestamp < :timestamp")
    suspend fun clearOldEvents(timestamp: String)
}
