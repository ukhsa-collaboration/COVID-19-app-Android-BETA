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

@Dao
interface ContactEventDao {
    @Insert
    fun insert(contactEvent: ContactEvent)

    @Update
    fun update(contactEvent: ContactEvent)

    @Transaction
    suspend fun createOrUpdate(newEvent: ContactEvent, errorMargin: Int) {
        val sorted = getAll().sortedBy { it.timestamp }
        Timber.d("all events $sorted")
        val eventsById = sorted.filter { it.sonarId.contentEquals(newEvent.sonarId) }
        for (event in eventsById.subList(0, eventsById.size)) {
            val newEventTime = DateTime(newEvent.timestamp, DateTimeZone.UTC)
            val storedEventTimeStart = DateTime(event.timestamp, DateTimeZone.UTC)
            val storedEventTimeEnd =
                DateTime(event.timestamp, DateTimeZone.UTC).plus(Seconds.seconds(event.duration))

            val rssis = event.rssiValues

            if (newEventTime.isBefore(storedEventTimeStart) &&
                newEventTime.isAfter(storedEventTimeStart.minus(Seconds.seconds(errorMargin)))
            ) {
                Timber.d(
                    "Updated event duration is ${event.duration + Seconds.secondsBetween(
                        storedEventTimeStart,
                        newEventTime
                    ).seconds}"
                )

                event.copy(
                    timestamp = newEvent.timestamp,
                    duration = event.duration + Seconds.secondsBetween(newEventTime, storedEventTimeStart).seconds,
                    rssiValues = newEvent.rssiValues.plus(rssis)
                )
                return
            } else if (newEventTime.isAfter(storedEventTimeEnd) &&
                newEventTime.isBefore(storedEventTimeEnd.plus(Seconds.seconds(errorMargin)))
            ) {
                Timber.d(
                    "Updated event duration is ${event.duration + Seconds.secondsBetween(
                        newEventTime,
                        storedEventTimeEnd
                    ).seconds}"
                )
                update(
                    event.copy(
                        duration = event.duration +
                            Seconds.secondsBetween(storedEventTimeEnd, newEventTime).seconds,
                        rssiValues = rssis.plus(newEvent.rssiValues)
                    )
                )
                return
            } else if (newEventTime.isAfter(storedEventTimeStart) &&
                newEventTime.isBefore(storedEventTimeEnd)
            ) {
                Timber.d("Updated event duration is unaffected")
                update(event.copy(rssiValues = rssis.plus(newEvent.rssiValues)))
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
    suspend fun clearOldEvents(timestamp: Long)
}
